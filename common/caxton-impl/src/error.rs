use jni::JNIEnv;
use std::error::Error;
use std::ffi::CStr;
use std::fmt::{Display, Formatter};
use std::str::Utf8Error;

#[derive(Debug)]
pub enum CxtError {
    Mine(anyhow::Error),
    Jni(jni::errors::Error),
    Char(Utf8Error),
    Null,
}

impl Display for CxtError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            CxtError::Mine(e) => Display::fmt(e, f),
            CxtError::Jni(e) => {
                f.write_str("JNI error: ")?;
                Display::fmt(e, f)
            }
            CxtError::Char(e) => Display::fmt(e, f),
            CxtError::Null => f.write_str("address was null"),
        }
    }
}

impl Error for CxtError {
    fn source(&self) -> Option<&(dyn Error + 'static)> {
        match self {
            CxtError::Mine(source) => Some(source.as_ref()),
            CxtError::Jni(source) => Some(source),
            CxtError::Char(source) => Some(source),
            CxtError::Null => None,
        }
    }
}

impl From<anyhow::Error> for CxtError {
    fn from(value: anyhow::Error) -> Self {
        Self::Mine(value)
    }
}

impl From<jni::errors::Error> for CxtError {
    fn from(value: jni::errors::Error) -> Self {
        Self::Jni(value)
    }
}

impl From<Utf8Error> for CxtError {
    fn from(value: Utf8Error) -> Self {
        Self::Char(value)
    }
}

impl CxtError {
    pub fn throw_exn(&self, env: &mut JNIEnv) -> jni::errors::Result<()> {
        match self {
            CxtError::Mine(err) => env.throw_new(
                "xyz/flirora/caxton/dll/CaxtonNativeException",
                format!("{err:?}"),
            ),
            CxtError::Jni(err) => env.throw_new("java/lang/RuntimeException", err.to_string()),
            CxtError::Char(err) => {
                env.throw_new("java/nio/charset/CharacterCodingException", err.to_string())
            }
            CxtError::Null => throw_null_ptr_exn(env),
        }
    }

    pub fn get_class(&self) -> &'static CStr {
        match self {
            CxtError::Mine(error) => c"xyz/flirora/caxton/dll/CaxtonNativeException",
            CxtError::Jni(error) => c"java/lang/RuntimeException",
            CxtError::Char(utf8_error) => c"java/nio/charset/CharacterCodingException",
            CxtError::Null => c"java/lang/NullPointerException",
        }
    }

    pub fn get_message(&self) -> String {
        match self {
            CxtError::Mine(err) => format!("{err:?}"),
            CxtError::Jni(err) => err.to_string(),
            CxtError::Char(err) => err.to_string(),
            CxtError::Null => "address was null".into(),
        }
    }
}

pub(crate) fn throw_null_ptr_exn(env: &mut JNIEnv) -> jni::errors::Result<()> {
    env.throw_new("java/lang/NullPointerException", "address was null")
}
