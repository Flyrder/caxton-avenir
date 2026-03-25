extern crate cbindgen;

use std::env;

use cbindgen::Language;

fn main() {
    let crate_dir = env::var("CARGO_MANIFEST_DIR").unwrap();

    cbindgen::Builder::new()
        .with_crate(crate_dir)
        .with_language(Language::C)
        .rename_item("anyhow::Error", "Caxton1AnyhowError")
        .with_include_version(true)
        .with_parse_deps(true)
        .with_parse_include(&["anyhow"])
        .with_parse_extra_bindings(&["anyhow"])
        .generate()
        .expect("Unable to generate bindings")
        .write_to_file("caxton1.h");
}
