use std::{
    fs::{self, OpenOptions},
    path::Path,
    time::Instant,
};

use anyhow::{anyhow, Context};
use base64::engine::general_purpose::URL_SAFE;
use base64::Engine;
use msdfgen::{Bitmap, FillRule, FontExt, Framing, MsdfGeneratorConfig, Pixel, Vector2};
use rayon::iter::{IndexedParallelIterator, IntoParallelIterator, ParallelIterator};
use rustybuzz::{Face, Tag};
use serde::{de, Deserialize};
use sha2::{Digest, Sha256};
use ttf_parser::{GlyphId, Rect, Variation};

use crate::tlist::TList;

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

#[derive(Deserialize, Debug)]
#[serde(default)]
pub struct FontOptions {
    pub shrinkage: f64,
    pub margin: Option<u32>,
    pub range: u32,
    pub invert: Option<bool>,
    pub variations: Vec<CxVariation>,
    pub face_index: u32,
}

impl FontOptions {
    fn margin(&self) -> u32 {
        self.margin.unwrap_or(self.range)
    }
}

impl Default for FontOptions {
    fn default() -> Self {
        Self {
            shrinkage: 32.0,
            margin: None,
            range: 4,
            invert: None,
            variations: Default::default(),
            face_index: 0,
        }
    }
}

/// Font information used to render text by Caxton.
pub struct Font<'a> {
    pub face: Face<'a>,
    pub tlist: Vec<TList>,
    pub bboxes: Vec<u64>,
    pub num_atlas_locations: usize,
}

impl<'a> Font<'a> {
    pub fn from_memory(
        contents: &'a [u8],
        cache_dir: &'_ Path,
        options: &FontOptions,
    ) -> anyhow::Result<Self> {
        let mut sha = Sha256::new();
        sha.update(contents);
        sha.update(&format!(
            "{} {} {} {}",
            options.shrinkage,
            options.margin(),
            options.range,
            match options.invert {
                None => "auto",
                Some(true) => "true",
                Some(false) => "false",
            },
        ));
        for var in &options.variations {
            sha.update(&format!(" {} {}", var.0.axis, var.0.value));
        }
        sha.update(SALT);
        let sha = sha.finalize();

        let mut this_cache = cache_dir.to_path_buf();
        this_cache.push(URL_SAFE.encode(sha) + ".zst");

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
                (rect.x_min as u16 as u64)
                    | (rect.y_min as u16 as u64) << 16
                    | (rect.x_max as u16 as u64) << 32
                    | (rect.y_max as u16 as u64) << 48
            })
            .collect();

        let tlist = (|| -> anyhow::Result<_> {
            let fh = OpenOptions::new().read(true).open(&this_cache);
            if let Ok(mut fh) = fh {
                match TList::load(&mut fh) {
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
                    if let Err(e) = tlist.save(&mut fh) {
                        eprintln!("warn: failed to save cached atlas: {e}")
                    }
                }
                Err(e) => eprintln!("warn: failed to save cached atlas: {e}"),
            }
            Ok(tlist)
        })()?;

        Ok(Font {
            face,
            tlist: vec![tlist],
            bboxes,
            num_atlas_locations: num_glyphs as usize,
        })
    }

    pub fn num_mipmap_layers(&self) -> usize {
        self.tlist.len()
    }
}

fn create_atlas(face: &Face, options: &FontOptions) -> anyhow::Result<TList> {
    let mut tlist = TList::new();

    let glyph_indices = (0..face.number_of_glyphs())
        .map(|glyph_idx| (glyph_idx, face.glyph_bounding_box(GlyphId(glyph_idx))))
        .collect::<Vec<_>>();

    let pending_entries = tlist.adds(
        glyph_indices
            .iter()
            .map(|(_, bbox)| dimensions_in_image(bbox, options)),
    )?;

    glyph_indices
        .into_par_iter()
        .zip_eq(pending_entries.into_par_iter())
        .try_for_each(|((glyph_idx, bbox), mut pe)| -> anyhow::Result<()> {
            let mtsdf = create_mtsdf(face, GlyphId(glyph_idx), bbox, options)?;
            pe.put(&mtsdf);
            anyhow::Result::Ok(())
        })?;

    Ok(tlist)
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
) -> anyhow::Result<Bitmap<msdfgen::Rgba<f32>>> {
    // This should be passed as a parameter, but MsdfGeneratorConfig isn’t Sync
    let msdf_config = MsdfGeneratorConfig::default();

    let bounding_box = match bounding_box {
        Some(s) => s,
        None => {
            return Ok(Bitmap::new(0, 0));
        }
    };

    let (width, height) = dimensions_in_image(&Some(bounding_box), options);

    let mut shape = face
        .as_ref()
        .glyph_shape(glyph_id)
        .ok_or_else(|| anyhow!("could not load glyph #{}", glyph_id.0))?;
    shape.edge_coloring_simple(3.0, 69);
    let framing: Framing<f64> = Framing::new(
        options.range as f64 * options.shrinkage,
        Vector2 {
            x: 1.0 / options.shrinkage,
            y: 1.0 / options.shrinkage,
        },
        Vector2 {
            x: options.margin() as f64 * options.shrinkage - bounding_box.x_min as f64,
            y: options.margin() as f64 * options.shrinkage - bounding_box.y_min as f64,
        },
    );

    let mut mtsdf: Bitmap<msdfgen::Rgba<f32>> = Bitmap::new(width, height);
    shape.generate_mtsdf(&mut mtsdf, &framing, &msdf_config);
    if options.invert.is_none() {
        shape.correct_sign(&mut mtsdf, &framing, FillRule::NonZero);
    }

    let mtsdf_pixels = mtsdf.pixels_mut();
    if options.invert == Some(true) {
        mtsdf_pixels.iter_mut().for_each(|pixel| pixel.invert());
    }

    Ok(mtsdf)
}
