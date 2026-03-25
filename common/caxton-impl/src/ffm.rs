use core::slice;
use std::{
    ffi::{c_char, CStr, CString},
    mem,
    path::PathBuf,
};

use anyhow::Context;
use nanoserde::DeJson;

use crate::{
    error::CxtError,
    font::{Font, FontInner, FontOptions},
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
    font_len: usize,
    cache_path: *const c_char,
    options: *const c_char,
) -> Caxton1Status<*mut Font<'static, 'static>> {
    let result = (|| -> Result<_, CxtError> {
        let font_data = unsafe { slice::from_raw_parts(font_data, font_len) };
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
