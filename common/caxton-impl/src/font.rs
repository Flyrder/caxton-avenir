use std::convert::AsRef;
use std::ops::Deref;
use std::str::Chars;
use std::{
    fs::{self, OpenOptions},
    path::Path,
    time::Instant,
};

use ab_glyph_rasterizer::{Point, Rasterizer};
use anyhow::Context;
use base64::engine::general_purpose::URL_SAFE;
use base64::Engine;
use fdsm::bezier::scanline::FillRule;
use fdsm::correct_error::{correct_error_mtsdf, ErrorCorrectionConfig};
use fdsm::generate::generate_mtsdf;
use fdsm::render::correct_sign_mtsdf;
use fdsm::shape::Shape;
use fdsm::transform::Transform;
use harfrust::{Shaper, ShaperData, ShaperInstance, Variation};
use image::{Pixel, Rgba32FImage};
use na::{Affine2, Similarity2, Vector2};
use nanoserde::{DeJson, DeJsonErr, DeJsonErrReason, DeJsonState};
use rayon::iter::{
    IndexedParallelIterator, IntoParallelIterator, IntoParallelRefIterator, ParallelIterator,
};
use sha2::{Digest, Sha256};
use skrifa::instance::Location;
use skrifa::metrics::BoundingBox;
use skrifa::outline::{DrawError, DrawSettings, OutlinePen};
use skrifa::prelude::Size;
use skrifa::raw::TableProvider;
use skrifa::setting::VariationSetting;
use skrifa::{FontRef, GlyphId, MetadataProvider, Tag};

use crate::extrude::Kernel;
use crate::tlist::{TList, TListPendingEntry};

const SALT: [u8; 4] = [0xE6, 0x26, 0x69, 0x11];

#[derive(DeJson)]
struct CxVariationFormat {
    axis: String,
    value: f32,
}

#[repr(transparent)]
#[derive(Debug, Copy, Clone)]
pub struct CxVariation(pub VariationSetting);

impl DeJson for CxVariation {
    fn de_json(state: &mut DeJsonState, input: &mut Chars) -> Result<Self, DeJsonErr> {
        let format = CxVariationFormat::de_json(state, input)?;
        let axis = Tag::new_checked(format.axis.as_bytes()).map_err(|e| DeJsonErr {
            msg: DeJsonErrReason::CannotParse(e.to_string()),
            line: state.line,
            col: state.col,
        })?;
        Ok(CxVariation(VariationSetting {
            selector: axis,
            value: format.value,
        }))
    }
}

#[derive(DeJson, Debug, Default, Copy, Clone, Eq, PartialEq)]
pub enum FontTech {
    #[default]
    #[nserde(rename = "msdf")]
    Msdf,
    #[nserde(rename = "raster")]
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

#[derive(DeJson, Debug)]
pub struct FontOptions {
    #[nserde(default = "FontTech::Msdf")]
    pub tech: FontTech,
    // TODO: implement alternative ways to specify scale?
    #[nserde(default = 32.0)]
    pub shrinkage: f64,
    #[nserde(default)]
    pub margin: Option<u32>,
    #[nserde(default = 4)]
    pub range: u32,
    #[nserde(default)]
    pub invert: Option<bool>,
    #[nserde(default)]
    pub variations: Vec<CxVariation>,
    #[nserde(default = 0)]
    pub face_index: u32,
    #[nserde(default = 0)]
    pub max_mipmap: u32,
}

impl FontOptions {
    fn margin(&self) -> u32 {
        self.margin.unwrap_or(self.range)
    }

    fn calculate_sha(&self, contents: &[u8]) -> [u8; 32] {
        let mut sha = Sha256::new();
        sha.update(contents);
        sha.update(format!(
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
            sha.update(format!(" {} {}", var.0.selector, var.0.value));
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

pub struct Font<'inner, 'data>
where
    'data: 'inner,
{
    pub inner: &'inner FontInner<'data>,
    pub shaper: Shaper<'inner>,
}

impl<'inner, 'data> Font<'inner, 'data>
where
    'data: 'inner,
{
    pub fn wrap(inner: &'inner FontInner<'data>) -> Font<'inner, 'data> {
        let shaper = inner
            .shaper_data
            .shaper(&inner.face)
            .instance(Some(&inner.shaper_instance))
            .build();
        Font { inner, shaper }
    }
}

impl<'inner, 'data> Deref for Font<'inner, 'data> {
    type Target = FontInner<'data>;

    #[inline]
    fn deref(&self) -> &Self::Target {
        self.inner
    }
}

/// Font information used to render text by Caxton.
pub struct FontInner<'a> {
    pub face: FontRef<'a>,
    pub tlist: Vec<TList>,
    pub bboxes: Vec<u64>,
    pub num_glyphs: usize,
    pub num_tlist_locations: usize,
    pub bytes_per_pixel: usize,
    pub axes: Location,
    shaper_data: ShaperData,
    shaper_instance: ShaperInstance,
}

impl<'a> FontInner<'a> {
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

        let face =
            FontRef::from_index(contents, options.face_index).context("failed to parse font")?;

        let axes = face
            .axes()
            .location(options.variations.iter().map(|var| var.0));

        let num_glyphs = face.maxp()?.num_glyphs();
        let bboxes = (0..num_glyphs)
            .map(|i| {
                let rect = face
                    .glyph_metrics(Size::unscaled(), &axes)
                    .bounds(GlyphId::new(i as u32))
                    .unwrap_or(BoundingBox::default());
                let rect = adjust_bbox(rect, options);
                (rect.x_min as u16 as u64)
                    | (rect.y_min as u16 as u64) << 16
                    | (rect.x_max as u16 as u64) << 32
                    | (rect.y_max as u16 as u64) << 48
            })
            .collect();

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

            eprintln!("Building atlas for {num_glyphs} glyphs; this might take a while");
            let before_atlas_construction = Instant::now();
            let tlist = create_atlas(&face, &axes, options)?;
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
                .truncate(true)
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

        let shaper_data = ShaperData::new(&face);
        let shaper_instance = ShaperInstance::from_variations(
            &face,
            options.variations.iter().map(|var| Variation {
                tag: var.0.selector,
                value: var.0.value,
            }),
        );

        Ok(FontInner {
            face,
            tlist,
            bboxes,
            num_glyphs: num_glyphs as usize,
            num_tlist_locations,
            bytes_per_pixel: options.tech.bytes_per_pixel(),
            axes,
            shaper_data,
            shaper_instance,
        })
    }

    pub fn num_mipmap_layers(&self) -> usize {
        self.tlist.len()
    }
}

fn create_atlas(
    face: &FontRef,
    axes: &Location,
    options: &FontOptions,
) -> anyhow::Result<Vec<TList>> {
    let glyph_indices_and_bboxes = (0..face.maxp()?.num_glyphs())
        .map(|glyph_idx| {
            let bbox = face
                .glyph_metrics(Size::unscaled(), axes)
                .bounds(GlyphId::from(glyph_idx));
            let bbox = bbox.map(|bbox| adjust_bbox(bbox, options));
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
                            let glyph_id = GlyphId::from(glyph_idx);
                            create_mtsdf(face, glyph_id, bbox, options, mipmap, &mut pe, axes)?;
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
                                let glyph_id = GlyphId::from(glyph_idx);
                                create_bitmap(
                                    face,
                                    glyph_id,
                                    bbox,
                                    options,
                                    mipmap,
                                    &mut pe,
                                    &mut pe_outline,
                                    &kernel,
                                    axes,
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
fn adjust_bbox(mut bounding_box: BoundingBox, options: &FontOptions) -> BoundingBox {
    let max_shrink_factor = options.shrinkage * (options.max_mipmap as f64).exp2();

    // Fun fact: I accidentally put x_max instead of y_max here at first
    let y_max_in_max_mipmap_atlas_pixels = (bounding_box.y_max as f64) / max_shrink_factor;
    let whole_atlas_pixels_above_baseline = y_max_in_max_mipmap_atlas_pixels.ceil();
    let font_units_above_baseline = whole_atlas_pixels_above_baseline * max_shrink_factor;
    bounding_box.y_max = font_units_above_baseline as f32;

    let y_min_in_max_mipmap_atlas_pixels = (bounding_box.y_min as f64) / max_shrink_factor;
    let whole_atlas_pixels_below_baseline = y_min_in_max_mipmap_atlas_pixels.floor();
    let font_units_below_baseline = whole_atlas_pixels_below_baseline * max_shrink_factor;
    bounding_box.y_min = font_units_below_baseline as f32;

    bounding_box
}

fn dimensions_in_image(bounding_box: &Option<BoundingBox>, options: &FontOptions) -> (u32, u32) {
    match bounding_box {
        None => (0, 0),
        Some(bounding_box) => {
            let margin = options.margin();
            let raw_width = bounding_box.x_max - bounding_box.x_min;
            let raw_height = bounding_box.y_max - bounding_box.y_min;
            let width = (raw_width.abs() as f64 / options.shrinkage).ceil() as u32 + 2 * margin;
            let height = (raw_height.abs() as f64 / options.shrinkage).ceil() as u32 + 2 * margin;
            (width, height)
        }
    }
}

fn create_mtsdf(
    face: &FontRef,
    glyph_id: GlyphId,
    bounding_box: Option<BoundingBox>,
    options: &FontOptions,
    mipmap: u32,
    pe: &mut TListPendingEntry,
    axes: &Location,
) -> anyhow::Result<()> {
    let Some(bounding_box) = bounding_box else {
        return Ok(());
    };

    let (width, height) = dimensions_in_image(&Some(bounding_box), options);
    let width = adjust_dims(width, mipmap);
    let height = adjust_dims(height, mipmap);

    let (mut shape, _) =
        fdsm_skrifa::load_shape_from_face(face, glyph_id, axes).context("Glyph not found")?;
    let framing: Affine2<f64> = framing(options, &bounding_box, mipmap);
    shape.transform(&framing);
    let shape = Shape::edge_coloring_simple(shape, 0.03, 69);
    let prepared_shape = shape.prepare();

    let mut mtsdf: Rgba32FImage = Rgba32FImage::new(width, height);
    generate_mtsdf(&prepared_shape, options.range as f64, &mut mtsdf);
    correct_error_mtsdf(
        &mut mtsdf,
        &shape,
        &prepared_shape,
        options.range as f64,
        &ErrorCorrectionConfig::default(),
    );
    if options.invert.is_none() {
        correct_sign_mtsdf(&mut mtsdf, &prepared_shape, FillRule::Nonzero);
    }

    if options.invert == Some(true) {
        let mtsdf_pixels = mtsdf.pixels_mut();
        mtsdf_pixels.for_each(|pixel| pixel.invert());
    }

    pe.put(&mtsdf);

    Ok(())
}

fn create_bitmap(
    face: &FontRef,
    glyph_id: GlyphId,
    bounding_box: Option<BoundingBox>,
    options: &FontOptions,
    mipmap: u32,
    pe: &mut TListPendingEntry,
    pe_outline: &mut TListPendingEntry,
    kernel: &Kernel,
    axes: &Location,
) -> anyhow::Result<()> {
    let Some(bounding_box) = bounding_box else {
        return Ok(());
    };
    let (width, height) = dimensions_in_image(&Some(bounding_box), options);
    let width = adjust_dims(width, mipmap);
    let height = adjust_dims(height, mipmap);
    let mut rasterizer = Rasterizer::new(width as usize, height as usize);
    let glyph = face
        .outline_glyphs()
        .get(glyph_id)
        .ok_or(DrawError::GlyphNotFound(glyph_id))?;
    glyph.draw(
        DrawSettings::unhinted(Size::unscaled(), axes),
        &mut AbOutlineBuilder {
            rasterizer: &mut rasterizer,
            open_point: None,
            last_point: None,
            bbox: &bounding_box,
            options,
            scale: (-(mipmap as f64)).exp2(),
        },
    )?;
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

fn framing(options: &FontOptions, bounding_box: &BoundingBox, mipmap: u32) -> Affine2<f64> {
    let mipmap_scale = (-(mipmap as f64)).exp2();
    let scale = mipmap_scale / options.shrinkage;
    nalgebra::convert::<_, Affine2<f64>>(Similarity2::new(
        Vector2::new(
            options.margin() as f64 * mipmap_scale - bounding_box.x_min as f64 * scale,
            options.margin() as f64 * mipmap_scale - bounding_box.y_min as f64 * scale,
        ),
        0.0,
        scale,
    ))
}

fn adjust_dims(n: u32, p: u32) -> u32 {
    (n + ((1 << p) - 1)) >> p
}

#[derive(Debug)]
struct AbOutlineBuilder<'a> {
    rasterizer: &'a mut Rasterizer,
    open_point: Option<Point>,
    last_point: Option<Point>,
    bbox: &'a BoundingBox,
    options: &'a FontOptions,
    scale: f64,
}

impl<'a> AbOutlineBuilder<'a> {
    fn point(&self, x: f32, y: f32) -> Point {
        let xp =
            ((x - self.bbox.x_min) as f64) / self.options.shrinkage + self.options.margin() as f64;
        let yp =
            ((y - self.bbox.y_min) as f64) / self.options.shrinkage + self.options.margin() as f64;
        let xp = (xp * self.scale) as f32;
        let yp = (yp * self.scale) as f32;
        (xp, yp).into()
    }
}

impl<'a> OutlinePen for AbOutlineBuilder<'a> {
    fn move_to(&mut self, x: f32, y: f32) {
        self.last_point = Some(self.point(x, y));
        self.open_point = self.last_point;
    }

    fn line_to(&mut self, x: f32, y: f32) {
        let end_point = self.point(x, y);
        self.rasterizer
            .draw_line(self.last_point.unwrap(), end_point);
        self.last_point = Some(end_point);
    }

    fn quad_to(&mut self, x1: f32, y1: f32, x: f32, y: f32) {
        let end_point = self.point(x, y);
        self.rasterizer
            .draw_quad(self.last_point.unwrap(), self.point(x1, y1), end_point);
        self.last_point = Some(end_point);
    }

    fn curve_to(&mut self, x1: f32, y1: f32, x2: f32, y2: f32, x: f32, y: f32) {
        let end_point = self.point(x, y);
        self.rasterizer.draw_cubic(
            self.last_point.unwrap(),
            self.point(x1, y1),
            self.point(x2, y2),
            end_point,
        );
        self.last_point = Some(end_point);
    }

    fn close(&mut self) {
        // Fix for issue #46
        if self.last_point != self.open_point {
            self.rasterizer
                .draw_line(self.last_point.unwrap(), self.open_point.unwrap());
        }
    }
}
