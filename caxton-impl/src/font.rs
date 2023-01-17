use std::{
    cmp::Reverse,
    fs::{self, DirBuilder},
    path::Path,
    time::Instant,
};

use anyhow::{anyhow, Context};
use base64::engine::general_purpose::URL_SAFE;
use base64::Engine;
use image::GenericImage;
use msdfgen::{Bitmap, FillRule, FontExt, Framing, MsdfGeneratorConfig, Pixel, Vector2};
use rustybuzz::{Face, GlyphBuffer, Tag, UnicodeBuffer};
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
    pub margin: u32,
    pub range: u32,
    pub invert: Option<bool>,
    pub variations: Vec<CxVariation>,
}

impl Default for FontOptions {
    fn default() -> Self {
        Self {
            shrinkage: 32.0,
            margin: 8,
            range: 4,
            invert: None,
            variations: Default::default(),
        }
    }
}

/// Font information used to render text by Caxton.
pub struct Font<'a> {
    pub face: Face<'a>,
    pub tlist: TList,
    pub bboxes: Vec<u64>,
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
            options.margin,
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
        this_cache.push(URL_SAFE.encode(sha));

        let mut face = Face::from_slice(contents, 0).context("failed to parse font")?;
        for var in &options.variations {
            face.set_variation(var.0.axis, var.0.value)
                .ok_or_else(|| anyhow!("axis {} not found", var.0.axis))?;
        }

        let bboxes = (0..face.number_of_glyphs())
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
            if this_cache.exists() && this_cache.is_dir() {
                match TList::load(&this_cache) {
                    Ok(atlas) => return Ok(atlas),
                    Err(e) => {
                        eprintln!("warn: loading atlas failed; regenerating");
                        eprintln!("({e})");
                    }
                }
            }

            eprintln!(
                "Building atlas for {} glyphs; this might take a while",
                face.number_of_glyphs()
            );
            let before_atlas_construction = Instant::now();
            let tlist = create_atlas(&face, options)?;
            let after_atlas_construction = Instant::now();
            eprintln!(
                "Built atlas in {} ms! ^_^",
                (after_atlas_construction - before_atlas_construction).as_millis()
            );
            if this_cache.exists() {
                fs::remove_file(&this_cache)
                    .or_else(|_| fs::remove_dir_all(&this_cache))
                    .context("failed to remove old cache data")?;
            }
            DirBuilder::new().recursive(true).create(&this_cache)?;
            eprintln!("saving atlas to {}...", this_cache.display());
            if let Err(e) = tlist.save(&this_cache) {
                eprintln!("warn: failed to save cached atlas: {e}")
            }
            Ok(tlist)
        })()?;

        Ok(Font {
            face,
            tlist,
            bboxes,
        })
    }
}

fn create_atlas(face: &Face, options: &FontOptions) -> anyhow::Result<TList> {
    let msdf_config = MsdfGeneratorConfig::default();

    let mut tlist = TList::new();

    let mut glyph_indices = (0..face.number_of_glyphs())
        .map(|glyph_idx| (glyph_idx, face.glyph_bounding_box(GlyphId(glyph_idx))))
        .collect::<Vec<_>>();
    glyph_indices.sort_by_key(|(_, bb)| bb.map(|bb| Reverse(bb.height())));

    for glyph_index in 0..face.number_of_glyphs() {
        let glyph_id = GlyphId(glyph_index);
        let bounding_box = face.glyph_bounding_box(glyph_id);

        let bounding_box = match bounding_box {
            Some(s) => s,
            None => {
                tlist.add_empty();
                continue;
            }
        };

        let width = (bounding_box.width().unsigned_abs() as f64 / options.shrinkage).ceil() as u32
            + 2 * options.margin;
        let height = (bounding_box.height().unsigned_abs() as f64 / options.shrinkage).ceil()
            as u32
            + 2 * options.margin;

        let mut shape = face
            .as_ref()
            .glyph_shape(glyph_id)
            .ok_or_else(|| anyhow!("could not load glyph #{glyph_index}"))?;
        shape.edge_coloring_simple(3.0, 69);
        let framing: Framing<f64> = Framing::new(
            options.range as f64 * options.shrinkage,
            Vector2 {
                x: 1.0 / options.shrinkage,
                y: 1.0 / options.shrinkage,
            },
            Vector2 {
                x: options.margin as f64 * options.shrinkage - bounding_box.x_min as f64,
                y: options.margin as f64 * options.shrinkage - bounding_box.y_min as f64,
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

        tlist.add(&mtsdf);
    }

    Ok(tlist)
}
