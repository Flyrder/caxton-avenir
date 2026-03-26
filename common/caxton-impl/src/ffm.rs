use core::slice;
use std::{
    ffi::{c_char, CStr, CString},
    mem::{self, MaybeUninit},
    path::PathBuf,
    ptr,
};

use anyhow::Context;
use harfrust::{BufferClusterLevel, Direction, Script, SerializeFlags, UnicodeBuffer};
use nanoserde::DeJson;
use skrifa::{prelude::Size, GlyphId, MetadataProvider, Tag};

use crate::{
    cfont::{ConfiguredFont, ConfiguredFontSettings},
    error::CxtError,
    font::{Font, FontInner, FontOptions},
    shape::{ShapingResult, ShapingResultEntry},
    tlist::TLocation,
    to_i16_clamped,
};

/// Passes errors from caxton-impl in a C-compatible form.
#[derive(Debug)]
#[repr(C)]
pub enum Caxton1Status<T> {
    /// Indicates a successful operation.
    Ok(T),
    /// Indicates an error.
    Err {
        /// The name of the class of exception to throw on Java.
        ///
        /// This points to a static string.
        cls: *const c_char,
        /// The message accompanying this error.
        ///
        /// This field is owned by the type and must be
        /// destroyed with [`caxton1_statusmsg_destroy`].
        message: *mut c_char,
    },
}

impl<T> Caxton1Status<T> {
    pub fn from_err(err: CxtError) -> Self {
        let mut message = err.get_message();
        message.push('\0');
        Self::Err {
            cls: err.get_class().as_ptr(),
            message: CString::from_vec_with_nul(message.into())
                .unwrap()
                .into_raw(),
        }
    }

    pub fn from_result(result: Result<T, CxtError>) -> Self {
        match result {
            Ok(value) => Self::Ok(value),
            Err(err) => Self::from_err(err),
        }
    }
}

impl<T> Drop for Caxton1Status<T> {
    fn drop(&mut self) {
        if let Caxton1Status::Err { cls: _, message } = *self {
            unsafe {
                caxton1_statusmsg_destroy(message);
            }
        }
    }
}

/// Frees a message string belonging to [`Caxton1Status`].
///
/// ## Safety
///
/// `message` must belong to an instance of [`Caxton1Status::Err`]
/// and must not have been previously passed to this function.
/// You can no longer access `message` after this function returns.
#[no_mangle]
pub unsafe extern "C" fn caxton1_statusmsg_destroy(message: *mut c_char) {
    mem::drop(CString::from_raw(message));
}

/// The version of the `caxton-impl` crate.
///
/// ## Safety
///
/// This is a `static mut` to allow it to be a `*const c_char`. It is always
/// safe to read; conversely, it should never be written to.
#[used]
#[no_mangle]
pub static mut caxton1_version: *const c_char = unsafe {
    CStr::from_bytes_with_nul_unchecked(concat!(env!("CARGO_PKG_VERSION"), "\0").as_bytes())
        .as_ptr()
};

/// Creates a Caxton font.
///
/// ## Safety
///
/// * `font_data` must be valid for `font_len` bytes and must remain so until the font is destroyed.
/// * `cache_path` and `options` must point to a valid, nul-ternimated C string.
#[no_mangle]
pub unsafe extern "C" fn caxton1_font_create(
    font_data: *const u8,
    font_len: u64,
    cache_path: *const c_char,
    options: *const c_char,
) -> Caxton1Status<*mut Font<'static, 'static>> {
    let result = (|| -> Result<_, CxtError> {
        let font_data = unsafe {
            slice::from_raw_parts(
                font_data,
                usize::try_from(font_len).context("font_len was too large")?,
            )
        };
        let cache_path = unsafe { CStr::from_ptr(cache_path) }.to_str()?;
        let options = unsafe { CStr::from_ptr(options) }.to_str()?;
        let options =
            FontOptions::deserialize_json(options).context("failed to decode options JSON")?;

        let font_inner = Box::new(
            FontInner::from_memory(font_data, &PathBuf::from(cache_path), &options)
                .context("font creation failed")?,
        );
        let font_inner = Box::leak(font_inner);

        let font_outer = Box::new(Font::wrap(font_inner));
        let font_outer = Box::leak(font_outer);
        Ok(font_outer as *mut _)
    })();
    Caxton1Status::from_result(result)
}

/// Destroys a Caxton font.
///
/// ## Safety
///
/// `font` must have been returned from [`caxton1_font_create`]
/// and must not have been passed to [`caxton1_font_destroy`]
/// since then.
#[no_mangle]
pub unsafe extern "C" fn caxton1_font_destroy(font: *mut Font) {
    let outer = Box::from_raw(font);
    let inner = outer.inner as *const FontInner;
    mem::drop(outer);
    mem::drop(Box::from_raw(inner.cast_mut()));
}

/// Gets the glyph index on this font corresponding to the given code point.
///
/// If `codepoint` does not correspond to a valid Unicode
/// scalar value or does not correpsond to a glyph in this
/// font, then `-1` will be returned.
///
/// ## Safety
///
/// `font` must have been returned from [`caxton1_font_create`]
/// and must not have been passed to [`caxton1_font_destroy`]
/// since then.
#[no_mangle]
pub extern "C" fn caxton1_font_codepoint_to_glyph_index(font: &Font, codepoint: u32) -> u32 {
    let codepoint = match char::try_from(codepoint) {
        Ok(codepoint) => codepoint,
        Err(_) => return u32::MAX,
    };
    font.face
        .charmap()
        .map(codepoint)
        .map(|x| x.to_u32())
        .unwrap_or(u32::MAX)
}

#[repr(C)]
pub struct Caxton1Metrics {
    pub values: [i16; 9],
}

#[no_mangle]
pub extern "C" fn caxton1_font_get_metrics(font: &Font) -> Caxton1Metrics {
    let face = &font.face;

    let metrics = face.metrics(Size::unscaled(), &font.axes);
    // TODO: compute these metrics using heuristics if they are not available
    let (underline_position, underline_thickness) = match &metrics.underline {
        Some(underline) => (
            to_i16_clamped(underline.offset),
            to_i16_clamped(underline.thickness),
        ),
        None => (-1, -1),
    };
    let (strikeout_position, strikeout_thickness) = match &metrics.strikeout {
        Some(strikeout) => (
            to_i16_clamped(strikeout.offset),
            to_i16_clamped(strikeout.thickness),
        ),
        None => (-1, -1),
    };
    let metrics = [
        metrics.units_per_em as i16,
        to_i16_clamped(metrics.ascent),
        to_i16_clamped(metrics.descent),
        to_i16_clamped(metrics.ascent - metrics.descent), // Height
        to_i16_clamped(metrics.leading),                  // Line gap
        underline_position,
        underline_thickness,
        strikeout_position,
        strikeout_thickness,
    ];
    Caxton1Metrics { values: metrics }
}

#[no_mangle]
pub extern "C" fn caxton1_font_num_mipmap_layers(font: &Font) -> i32 {
    font.num_mipmap_layers() as i32
}

#[no_mangle]
pub extern "C" fn caxton1_font_num_glyphs(font: &Font) -> i32 {
    font.num_glyphs as i32
}

#[no_mangle]
pub extern "C" fn caxton1_font_num_tlist_locations(font: &Font) -> i32 {
    font.num_tlist_locations as i32
}

#[no_mangle]
pub extern "C" fn caxton1_font_num_bytes_per_pixel(font: &Font) -> i32 {
    font.bytes_per_pixel as i32
}

/// Gets a pointer to the array of glyph bounding boxes.
///
/// The result is valid for `caxton1_font_num_glyphs(font)`
/// u64s as long as `font` is not deallocated (or mutated).
#[no_mangle]
pub extern "C" fn caxton1_font_get_bounding_boxes(font: &Font) -> *const u64 {
    assert!(font.bboxes.len() >= font.num_glyphs);
    font.bboxes.as_ptr()
}

/// Gets a pointer to the array of tlist locations for a given mipmap level.
///
/// The result is valid for `caxton1_font_num_tlist_locations(font)`
/// `TLocation`s as long as `font` is not deallocated (or mutated).
#[no_mangle]
pub extern "C" fn caxton1_font_get_tlist_pointer(font: &Font, mipmap: i32) -> *const TLocation {
    let locations = font.tlist[mipmap as usize].locations();
    assert!(locations.len() >= font.num_tlist_locations);
    locations.as_ptr()
}

/// Gets a pointer to the pixel data for a given mipmap level.
///
/// The number of bytes in the pixel data will be written to
/// `size_out`, which must not be null.
///
/// The result is valid as long as `font` is not deallocated (or mutated).
#[no_mangle]
pub extern "C" fn caxton1_font_get_pixel_data(
    font: &Font,
    mipmap: i32,
    size_out: &mut MaybeUninit<u64>,
) -> *const u8 {
    let pixel_data = font.tlist[mipmap as usize].data();
    size_out.write(pixel_data.len() as u64);
    pixel_data.as_ptr()
}

/// Gets the glyph name corresponding to the given glyph ID.
///
/// The number of bytes in the name will be written to `size_out`.
/// The pointer returned is **not** null-terminated.
///
/// If the given glyph does not exist or is unnamed, then
/// `*size_out` will be set to 0 and the return value is null.
#[no_mangle]
pub extern "C" fn caxton1_font_get_glyph_name(
    font: &Font,
    glyph_id: u32,
    len_out: &mut MaybeUninit<u64>,
) -> *const u8 {
    let name = font.face.glyph_names().get(GlyphId::from(glyph_id));
    match name {
        Some(name) => {
            let name_str = name.as_str();
            len_out.write(name_str.len() as u64);
            name_str.as_ptr()
        }
        None => {
            len_out.write(0);
            ptr::null()
        }
    }
}

#[repr(C)]
pub struct Caxton1DebugShapeInfo {
    /// Free with [`caxton1_statusmsg_destroy`] if not null.
    inferred_language: *const c_char,
    /// Free with [`caxton1_statusmsg_destroy`].
    serialized_results: *const c_char,
    inferred_script: u32,
    inferred_direction: Caxton1Direction,
}

#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug)]
#[repr(u8)]
pub enum Caxton1Direction {
    Invalid,
    LeftToRight,
    RightToLeft,
    TopToBottom,
    BottomToTop,
}

/// ## Safety
///
/// `text` must be a valid nul-terminated UTF-8 string.
#[no_mangle]
pub unsafe extern "C" fn caxton1_font_shape_for_debug(
    font: &Font,
    text: *const c_char,
) -> Caxton1DebugShapeInfo {
    let text = unsafe { str::from_utf8_unchecked(CStr::from_ptr(text).to_bytes()) };
    let mut buffer = UnicodeBuffer::new();
    buffer.push_str(text);
    buffer.guess_segment_properties();
    buffer.set_cluster_level(BufferClusterLevel::MonotoneCharacters);

    let inferred_language = match buffer.language() {
        Some(lang) => {
            let mut s = String::from(lang.as_str());
            s.push('\0');
            CString::from_vec_with_nul(s.into()).unwrap().into_raw()
        }
        None => ptr::null(),
    };
    let inferred_script = u32::from_le_bytes(buffer.script().tag().into_bytes());
    let inferred_direction = match buffer.direction() {
        Direction::Invalid => Caxton1Direction::Invalid,
        Direction::LeftToRight => Caxton1Direction::LeftToRight,
        Direction::RightToLeft => Caxton1Direction::RightToLeft,
        Direction::TopToBottom => Caxton1Direction::TopToBottom,
        Direction::BottomToTop => Caxton1Direction::BottomToTop,
    };

    let buffer = font.shaper.shape(buffer, &[]);
    let serialized_results = {
        let mut s = buffer.serialize(&font.shaper, SerializeFlags::default());
        s.push('\0');
        CString::from_vec_with_nul(s.into()).unwrap().into_raw()
    };

    Caxton1DebugShapeInfo {
        inferred_language,
        inferred_script,
        inferred_direction,
        serialized_results,
    }
}

/// Creates a new configured font.
///
/// ## Safety
///
/// * `settings` must be valid and nul-terminated.
#[no_mangle]
pub unsafe extern "C" fn caxton1_font_configure<'font>(
    font: &'font Font,
    settings: *const c_char,
) -> Caxton1Status<*mut ConfiguredFont<'font>> {
    Caxton1Status::from_result((|| -> Result<_, CxtError> {
        let settings = if !settings.is_null() {
            ConfiguredFontSettings::deserialize_json(unsafe {
                str::from_utf8(CStr::from_ptr(settings).to_bytes())
                    .context("string decoding failed")?
            })
            .context("failed to parse options")?
        } else {
            ConfiguredFontSettings::default()
        };
        let configured_font = Box::new(ConfiguredFont { font, settings });
        Ok(Box::into_raw(configured_font))
    })())
}

/// Destroys a configured font.
///
/// ## Safety
///
/// `font` must have been returned from [`caxton1_font_create`]
/// and must not have been passed to [`caxton1_font_destroy`]
/// since then.
#[no_mangle]
pub unsafe extern "C" fn caxton1_cfont_destroy(cfont: *mut ConfiguredFont) {
    mem::drop(Box::from_raw(cfont))
}

#[repr(C)]
pub struct Caxton1ShapingResult {
    data_count: u64,
    /// Destroy with [`caxton1_free_shaping_result_entries`].
    data: *mut ShapingResultEntry,
    total_width: i32,
}

/// Shapes a run of text.
///
/// ## Safety
///
/// `s` must be valid for `s_len` elements`.
///
/// ## Panics
///
/// Panics if `start > end` or `end > s_len`.
#[no_mangle]
pub unsafe extern "C" fn caxton1_cfont_shape(
    cfont: &ConfiguredFont,
    s: *const u16,
    s_len: u64,
    start: u64,
    end: u64,
    rtl: bool,
    script: u32,
) -> Caxton1ShapingResult {
    let string = slice::from_raw_parts(s, s_len as usize);
    let mut buffer = UnicodeBuffer::new(); // TODO: share buffer between different shaping runs
    let script = Script::from_iso15924_tag(Tag::from_u32(script));
    let substring = &string[(start as usize)..(end as usize)];
    {
        let mut i = 0;
        for c in char::decode_utf16(substring.iter().copied()) {
            let c = c.unwrap_or(char::REPLACEMENT_CHARACTER);
            buffer.add(c, i);
            i += c.len_utf16() as u32;
        }
    }
    buffer.set_direction(if rtl {
        Direction::RightToLeft
    } else {
        Direction::LeftToRight
    });
    if let Some(script) = script {
        buffer.set_script(script);
    }
    buffer.guess_segment_properties();
    buffer.set_cluster_level(BufferClusterLevel::MonotoneCharacters);

    let shaped = cfont.shape(mem::take(&mut buffer));
    let sr = ShapingResult::from_glyph_buffer(&shaped);

    let data = Box::into_raw(sr.data.into_boxed_slice());
    Caxton1ShapingResult {
        data_count: data.len() as u64,
        data: data.cast(),
        total_width: sr.total_width,
    }
}

/// Destroys a [`Caxton1ShapingResult`].
///
/// # Safety
///
/// `shaping_result` must have been returned from [`caxton1_cfont_shape`].
#[no_mangle]
pub unsafe extern "C" fn caxton1_shapingresult_destroy(shaping_result: &mut Caxton1ShapingResult) {
    let count = shaping_result.data_count;
    let data = shaping_result.data;
    caxton1_free_shaping_result_entries(data, count);
}

/// Frees the memory allocated in a [`Caxton1ShapingResult`].
///
/// # Safety
///
/// `data` and `count` must have originated from a [`Caxton1ShapingResult`]
/// returned from [`caxton1_cfont_shape`].
#[no_mangle]
pub unsafe extern "C" fn caxton1_free_shaping_result_entries(
    data: *mut ShapingResultEntry,
    count: u64,
) {
    let data_slice = ptr::slice_from_raw_parts_mut(data, count as usize);
    mem::drop(Box::from_raw(data_slice));
}
