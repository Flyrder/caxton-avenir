use std::error::Error;
use std::fmt::{Display, Formatter};
use jni::JNIEnv;

#[derive(Debug)]
pub enum CxtError {
    Mine(anyhow::Error),
    Jni(jni::errors::Error),
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
            CxtError::Null => f.write_str("address was null"),
        }
    }
}

impl Error for CxtError {
    fn source(&self) -> Option<&(dyn Error + 'static)> {
        match self {
            CxtError::Mine(source) => Some(source.as_ref()),
            CxtError::Jni(source) => Some(source),
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

impl CxtError {
    pub fn throw_exn(&self, env: &mut JNIEnv) -> jni::errors::Result<()> {
        match self {
            CxtError::Mine(err) => env.throw_new("xyz/flirora/caxton/dll/CaxtonNativeException", format!("{err:?}")),
            CxtError::Jni(err) => env.throw_new("java/lang/RuntimeException", err.to_string()),
            CxtError::Null => throw_null_ptr_exn(env)
        }
    }
}

pub(crate) fn throw_null_ptr_exn(env: &mut JNIEnv) -> jni::errors::Result<()> {
    env.throw_new("java/lang/NullPointerException", "address was null")
}
