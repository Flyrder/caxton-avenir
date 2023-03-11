use std::{
    fs::{self, OpenOptions},
    path::Path,
    time::Instant,
};

use ab_glyph_rasterizer::{Point, Rasterizer};
use anyhow::{anyhow, Context};
use base64::engine::general_purpose::URL_SAFE;
use base64::Engine;
use msdfgen::{Bitmap, FillRule, FontExt, Framing, MsdfGeneratorConfig, Pixel, Vector2};
use rayon::iter::{
    IndexedParallelIterator, IntoParallelIterator, IntoParallelRefIterator, ParallelIterator,
};
use rustybuzz::{Face, Tag};
use serde::{de, Deserialize};
use sha2::{Digest, Sha256};
use ttf_parser::{GlyphId, OutlineBuilder, Rect, Variation};

use crate::extrude::Kernel;
use crate::tlist::{TList, TListPendingEntry};

const SALT: [u8; 4] = [0xE6, 0x26, 0x69, 0x11];

#[derive(Deserialize)]
struct CxVariationFormat<'a> {
    axis: &'a str,
    value: f32,
}

#[repr(transparent)]
#[derive(Debug, Copy, Clone)]
pub struct CxVariation(pub Variation);

impl<'de> Deserialize<'de> for CxVariation {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        let format = CxVariationFormat::deserialize(deserializer)?;
        let axis = if format.axis.len() == 4 {
            Tag::from_bytes_lossy(format.axis.as_bytes())
        } else {
            return Err(de::Error::invalid_value(
                de::Unexpected::Str(format.axis),
                &"a string of 4 characters",
            ));
        };
        Ok(CxVariation(Variation {
            axis,
            value: format.value,
        }))
    }
}

#[derive(Deserialize, Debug, Default, Copy, Clone, Eq, PartialEq)]
pub enum FontTech {
    #[default]
    #[serde(rename = "msdf")]
    Msdf,
    #[serde(rename = "raster")]
    Raster,
}

impl FontTech {
    pub fn bytes_per_pixel(self) -> usize {
        match self {
            FontTech::Msdf => 4,
            FontTech::Raster => 1,
        }
    }
}

#[derive(Deserialize, Debug)]
#[serde(default)]
pub struct FontOptions {
    pub tech: FontTech,
    // TODO: implement alternative ways to specify scale?
    pub shrinkage: f64,
    pub margin: Option<u32>,
    pub range: u32,
    pub invert: Option<bool>,
    pub variations: Vec<CxVariation>,
    pub face_index: u32,
    pub max_mipmap: u32,
}

impl FontOptions {
    fn margin(&self) -> u32 {
        self.margin.unwrap_or(self.range)
    }

    fn calculate_sha(&self, contents: &[u8]) -> [u8; 32] {
        let mut sha = Sha256::new();
        sha.update(contents);
        sha.update(&format!(
            "{} {} {} {} {}{:?}",
            self.shrinkage,
            self.margin(),
            self.range,
            match self.invert {
                None => "auto",
                Some(true) => "true",
                Some(false) => "false",
            },
            self.max_mipmap,
            match self.tech {
                FontTech::Msdf => "",
                FontTech::Raster => " for Robin",
            },
        ));
        for var in &self.variations {
            sha.update(&format!(" {} {}", var.0.axis, var.0.value));
        }
        sha.update(SALT);
        sha.finalize().into()
    }
}

impl Default for FontOptions {
    fn default() -> Self {
        Self {
            tech: FontTech::Msdf,
            shrinkage: 32.0,
            margin: None,
            range: 4,
            invert: None,
            variations: Default::default(),
            face_index: 0,
            max_mipmap: 0,
        }
    }
}

/// Font information used to render text by Caxton.
pub struct Font<'a> {
    pub face: Face<'a>,
    pub tlist: Vec<TList>,
    pub bboxes: Vec<u64>,
    pub num_glyphs: usize,
    pub num_tlist_locations: usize,
    pub bytes_per_pixel: usize,
}

impl<'a> Font<'a> {
    pub fn from_memory(
        contents: &'a [u8],
        cache_dir: &'_ Path,
        options: &FontOptions,
    ) -> anyhow::Result<Self> {
        if options.tech == FontTech::Msdf && options.max_mipmap > 0 {
            eprintln!("adding mipmap layers for MSDF fonts is not recommended");
        }

        let sha = options.calculate_sha(contents);

        let mut this_cache = cache_dir.to_path_buf();
        this_cache.push(URL_SAFE.encode(sha) + ".zz");

        let mut face =
            Face::from_slice(contents, options.face_index).context("failed to parse font")?;
        for var in &options.variations {
            face.set_variation(var.0.axis, var.0.value)
                .ok_or_else(|| anyhow!("axis {} not found", var.0.axis))?;
        }

        let num_glyphs = face.number_of_glyphs();
        let bboxes = (0..num_glyphs)
            .map(|i| {
                let rect = face.glyph_bounding_box(GlyphId(i)).unwrap_or(Rect {
                    x_min: 0,
                    y_min: 0,
                    x_max: 0,
                    y_max: 0,
                });
                let rect = adjust_bbox(rect, &options);
                (rect.x_min as u16 as u64)
                    | (rect.y_min as u16 as u64) << 16
                    | (rect.x_max as u16 as u64) << 32
                    | (rect.y_max as u16 as u64) << 48
            })
            .collect();

        // TODO: generate multiple mipmap layers when requested;
        // implement conventional rasterization
        let tlist = (|| -> anyhow::Result<_> {
            let fh = OpenOptions::new().read(true).open(&this_cache);
            if let Ok(mut fh) = fh {
                match TList::load(&mut fh, (options.max_mipmap as usize) + 1) {
                    Ok(atlas) => return Ok(atlas),
                    Err(e) => {
                        eprintln!("warn: loading atlas failed; regenerating");
                        eprintln!("({e})");
                    }
                }
            }

            eprintln!(
                "Building atlas for {} glyphs; this might take a while",
                num_glyphs
            );
            let before_atlas_construction = Instant::now();
            let tlist = create_atlas(&face, options)?;
            let after_atlas_construction = Instant::now();
            eprintln!(
                "Built atlas in {} ms! ^_^",
                (after_atlas_construction - before_atlas_construction).as_millis()
            );
            eprintln!("saving atlas to {}...", this_cache.display());
            fs::create_dir_all(cache_dir)?;
            let fh = OpenOptions::new()
                .write(true)
                .create(true)
                .open(&this_cache);
            match fh {
                Ok(mut fh) => {
                    if let Err(e) = TList::save(&tlist, &mut fh) {
                        eprintln!("warn: failed to save cached atlas: {e}")
                    }
                }
                Err(e) => eprintln!("warn: failed to save cached atlas: {e}"),
            }
            Ok(tlist)
        })()?;

        // Hope and pray that all the tlists have the same number of locations
        let num_tlist_locations = tlist[0].locations().len();

        Ok(Font {
            face,
            tlist,
            bboxes,
            num_glyphs: num_glyphs as usize,
            num_tlist_locations,
            bytes_per_pixel: options.tech.bytes_per_pixel(),
        })
    }

    pub fn num_mipmap_layers(&self) -> usize {
        self.tlist.len()
    }
}

fn create_atlas(face: &Face, options: &FontOptions) -> anyhow::Result<Vec<TList>> {
    let glyph_indices_and_bboxes = (0..face.number_of_glyphs())
        .map(|glyph_idx| {
            let bbox = face.glyph_bounding_box(GlyphId(glyph_idx));
            let bbox = bbox.map(|bbox| adjust_bbox(bbox, &options));
            (glyph_idx, bbox)
        })
        .collect::<Vec<_>>();

    (0..=options.max_mipmap)
        .map(|mipmap| {
            let mut tlist = TList::new();

            let sizes = glyph_indices_and_bboxes.iter().map(|(_, bbox)| {
                let (width, height) = dimensions_in_image(bbox, options);
                (adjust_dims(width, mipmap), adjust_dims(height, mipmap))
            });

            match options.tech {
                FontTech::Msdf => {
                    let pending_entries = tlist.adds(4, sizes)?;

                    glyph_indices_and_bboxes
                        .par_iter()
                        .copied()
                        .zip_eq(pending_entries.into_par_iter())
                        .try_for_each(|((glyph_idx, bbox), mut pe)| -> anyhow::Result<()> {
                            let glyph_id = GlyphId(glyph_idx);
                            create_mtsdf(face, glyph_id, bbox, options, mipmap, &mut pe)?;
                            anyhow::Result::Ok(())
                        })?;
                }
                FontTech::Raster => {
                    let kernel =
                        Kernel::with_radius(options.range as f64 * (-(mipmap as f64)).exp2());
                    let pending_entries = tlist.adds_pairs(1, sizes)?;

                    glyph_indices_and_bboxes
                        .par_iter()
                        .copied()
                        .zip_eq(pending_entries.into_par_iter())
                        .try_for_each(
                            |((glyph_idx, bbox), (mut pe, mut pe_outline))| -> anyhow::Result<()> {
                                let glyph_id = GlyphId(glyph_idx);
                                create_bitmap(
                                    face,
                                    glyph_id,
                                    bbox,
                                    options,
                                    mipmap,
                                    &mut pe,
                                    &mut pe_outline,
                                    &kernel,
                                )?;
                                anyhow::Result::Ok(())
                            },
                        )?;
                }
            }

            Ok(tlist)
        })
        .collect()
}

/// Adjust the bounds of the bounding box so that the baseline is aligned to
/// 2^n pixels, where n is the maximum mipmap level.
fn adjust_bbox(mut bounding_box: Rect, options: &FontOptions) -> Rect {
    let max_shrink_factor = options.shrinkage * (options.max_mipmap as f64).exp2();

    // Fun fact: I accidentally put x_max instead of y_max here at first
    let y_max_in_max_mipmap_atlas_pixels = (bounding_box.y_max as f64) / max_shrink_factor;
    let whole_atlas_pixels_above_baseline = y_max_in_max_mipmap_atlas_pixels.ceil();
    let font_units_above_baseline = whole_atlas_pixels_above_baseline * max_shrink_factor;
    bounding_box.y_max = font_units_above_baseline.clamp(i16::MIN as f64, i16::MAX as f64) as i16;

    let y_min_in_max_mipmap_atlas_pixels = (bounding_box.y_min as f64) / max_shrink_factor;
    let whole_atlas_pixels_below_baseline = y_min_in_max_mipmap_atlas_pixels.floor();
    let font_units_below_baseline = whole_atlas_pixels_below_baseline * max_shrink_factor;
    bounding_box.y_min = font_units_below_baseline.clamp(i16::MIN as f64, i16::MAX as f64) as i16;

    bounding_box
}

fn dimensions_in_image(bounding_box: &Option<Rect>, options: &FontOptions) -> (u32, u32) {
    match bounding_box {
        None => (0, 0),
        Some(bounding_box) => {
            let margin = options.margin();
            let width = (bounding_box.width().unsigned_abs() as f64 / options.shrinkage).ceil()
                as u32
                + 2 * margin;
            let height = (bounding_box.height().unsigned_abs() as f64 / options.shrinkage).ceil()
                as u32
                + 2 * margin;
            (width, height)
        }
    }
}

fn create_mtsdf(
    face: &Face,
    glyph_id: GlyphId,
    bounding_box: Option<Rect>,
    options: &FontOptions,
    mipmap: u32,
    pe: &mut TListPendingEntry,
) -> anyhow::Result<()> {
    // This should be passed as a parameter, but MsdfGeneratorConfig isn’t Sync
    let msdf_config = MsdfGeneratorConfig::default();

    let Some(bounding_box) = bounding_box else { return Ok(()); };

    let (width, height) = dimensions_in_image(&Some(bounding_box), options);
    let width = adjust_dims(width, mipmap);
    let height = adjust_dims(height, mipmap);

    let mut shape = face
        .as_ref()
        .glyph_shape(glyph_id)
        .ok_or_else(|| anyhow!("could not load glyph #{}", glyph_id.0))?;
    shape.edge_coloring_simple(3.0, 69);
    let framing: Framing<f64> = framing(options, &bounding_box, mipmap);

    let mut mtsdf: Bitmap<msdfgen::Rgba<f32>> = Bitmap::new(width, height);
    shape.generate_mtsdf(&mut mtsdf, &framing, &msdf_config);
    if options.invert.is_none() {
        shape.correct_sign(&mut mtsdf, &framing, FillRule::NonZero);
    }

    let mtsdf_pixels = mtsdf.pixels_mut();
    if options.invert == Some(true) {
        mtsdf_pixels.iter_mut().for_each(|pixel| pixel.invert());
    }

    pe.put(&mtsdf);

    Ok(())
}

fn create_bitmap(
    face: &Face,
    glyph_id: GlyphId,
    bounding_box: Option<Rect>,
    options: &FontOptions,
    mipmap: u32,
    pe: &mut TListPendingEntry,
    pe_outline: &mut TListPendingEntry,
    kernel: &Kernel,
) -> anyhow::Result<()> {
    let Some(bounding_box) = bounding_box else { return Ok(()); };
    let (width, height) = dimensions_in_image(&Some(bounding_box), options);
    let width = adjust_dims(width, mipmap);
    let height = adjust_dims(height, mipmap);
    let mut rasterizer = Rasterizer::new(width as usize, height as usize);
    face.outline_glyph(
        glyph_id,
        &mut AbOutlineBuilder {
            rasterizer: &mut rasterizer,
            last_point: None,
            bbox: &bounding_box,
            options: &options,
            scale: (-(mipmap as f64)).exp2(),
        },
    );
    let slice = pe.as_slice_mut();
    rasterizer.for_each_pixel_2d(|x, y, alpha| {
        let index = (x as usize) + ((height - 1 - y) as usize) * (width as usize);
        slice[index] = (alpha * 255.0) as u8;
    });

    kernel.convolve(
        slice,
        pe_outline.as_slice_mut(),
        width as usize,
        height as usize,
    );

    Ok(())
}

fn framing(options: &FontOptions, bounding_box: &Rect, mipmap: u32) -> Framing<f64> {
    let scale = (-(mipmap as f64)).exp2() / options.shrinkage;
    Framing::new(
        options.range as f64 * options.shrinkage,
        Vector2 { x: scale, y: scale },
        Vector2 {
            x: options.margin() as f64 * options.shrinkage - bounding_box.x_min as f64,
            y: options.margin() as f64 * options.shrinkage - bounding_box.y_min as f64,
        },
    )
}

fn adjust_dims(n: u32, p: u32) -> u32 {
    (n + ((1 << p) - 1)) >> p
}

#[derive(Debug)]
struct AbOutlineBuilder<'a> {
    rasterizer: &'a mut Rasterizer,
    last_point: Option<Point>,
    bbox: &'a Rect,
    options: &'a FontOptions,
    scale: f64,
}

impl<'a> AbOutlineBuilder<'a> {
    fn point(&self, x: f32, y: f32) -> Point {
        let xp = ((x - (self.bbox.x_min as f32)) as f64) / self.options.shrinkage
            + self.options.margin() as f64;
        let yp = ((y - (self.bbox.y_min as f32)) as f64) / self.options.shrinkage
            + self.options.margin() as f64;
        let xp = (xp * self.scale) as f32;
        let yp = (yp * self.scale) as f32;
        (xp, yp).into()
    }
}

impl<'a> OutlineBuilder for AbOutlineBuilder<'a> {
    fn move_to(&mut self, x: f32, y: f32) {
        self.last_point = Some(self.point(x, y));
    }

    fn line_to(&mut self, x: f32, y: f32) {
        self.rasterizer
            .draw_line(self.last_point.unwrap(), self.point(x, y));
        self.last_point = Some(self.point(x, y));
    }

    fn quad_to(&mut self, x1: f32, y1: f32, x: f32, y: f32) {
        self.rasterizer.draw_quad(
            self.last_point.unwrap(),
            self.point(x1, y1),
            self.point(x, y),
        );
        self.last_point = Some(self.point(x, y));
    }

    fn curve_to(&mut self, x1: f32, y1: f32, x2: f32, y2: f32, x: f32, y: f32) {
        self.rasterizer.draw_cubic(
            self.last_point.unwrap(),
            self.point(x1, y1),
            self.point(x2, y2),
            self.point(x, y),
        );
        self.last_point = Some(self.point(x, y));
    }

    fn close(&mut self) {}
}
