use std::error::Error;
use std::ffi::CStr;
use std::fmt::{Display, Formatter};
use std::str::Utf8Error;

#[derive(Debug)]
pub enum CxtError {
    Mine(anyhow::Error),
    Char(Utf8Error),
    Null,
}

impl Display for CxtError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            CxtError::Mine(e) => Display::fmt(e, f),
            CxtError::Char(e) => Display::fmt(e, f),
            CxtError::Null => f.write_str("address was null"),
        }
    }
}

impl Error for CxtError {
    fn source(&self) -> Option<&(dyn Error + 'static)> {
        match self {
            CxtError::Mine(source) => Some(source.as_ref()),
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

impl From<Utf8Error> for CxtError {
    fn from(value: Utf8Error) -> Self {
        Self::Char(value)
    }
}

impl CxtError {
    pub fn get_class(&self) -> &'static CStr {
        match self {
            CxtError::Mine(_) => c"xyz/flirora/caxton/dll/CaxtonNativeException",
            CxtError::Char(_) => c"java/nio/charset/CharacterCodingException",
            CxtError::Null => c"java/lang/NullPointerException",
        }
    }

    pub fn get_message(&self) -> String {
        match self {
            CxtError::Mine(err) => format!("{err:?}"),
            CxtError::Char(err) => err.to_string(),
            CxtError::Null => "address was null".into(),
        }
    }
}
