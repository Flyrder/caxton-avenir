use std::{slice, str::FromStr};
use std::str::Chars;

use rustybuzz::{Feature, GlyphBuffer, UnicodeBuffer};
use nanoserde::{DeJson, DeJsonErr, DeJsonState};

use crate::font::Font;

#[repr(transparent)]
#[derive(Copy, Clone, Debug)]
pub struct CxFeature(pub Feature);

impl DeJson for CxFeature {
    fn de_json(state: &mut DeJsonState, input: &mut Chars) -> Result<Self, DeJsonErr> {
        let feature_string = String::de_json(state, input)?;
        let feature = Feature::from_str(&feature_string).map_err(|_e| {
            state.err_parse("feature string")
        })?;
        Ok(CxFeature(feature))
    }
}

#[derive(DeJson)]
#[nserde(default)]
pub struct ConfiguredFontSettings {
    pub features: Vec<CxFeature>,
}

#[allow(clippy::derivable_impls)]
impl Default for ConfiguredFontSettings {
    fn default() -> Self {
        Self {
            features: Default::default(),
        }
    }
}

pub struct ConfiguredFont<'font> {
    pub font: &'font Font<'font>,
    pub settings: ConfiguredFontSettings,
}

impl<'font> ConfiguredFont<'font> {
    pub fn shape(&self, buffer: UnicodeBuffer) -> GlyphBuffer {
        // TODO: provide way to configure features in font
        let features = &self.settings.features;
        // SAFETY: CxFeature and Feature are layout-compatible
        let features = unsafe { slice::from_raw_parts(features.as_ptr().cast(), features.len()) };
        rustybuzz::shape(&self.font.face, features, buffer)
    }
}
