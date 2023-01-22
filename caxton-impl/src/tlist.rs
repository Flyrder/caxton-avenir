use std::fs::OpenOptions;
use std::io::{Read, Write};
use std::path::Path;
use std::{mem, slice};

use anyhow::{bail, Context};
use io_ext::{ReadExt, WriteExt};
use msdfgen::Bitmap;

#[repr(C)]
#[derive(Copy, Clone, Eq, PartialEq, Debug, Default)]
pub struct TLocation {
    packed: u64,
}

impl TLocation {
    pub fn new(offset: u32, width: u16, height: u16) -> Self {
        TLocation {
            packed: ((offset as u64) << 32) | (width as u64) | ((height as u64) << 16),
        }
    }

    pub fn offset(self) -> u32 {
        (self.packed >> 32) as u32
    }

    pub fn width(self) -> u16 {
        self.packed as u16
    }

    pub fn height(self) -> u16 {
        (self.packed >> 16) as u16
    }
}

#[derive(Clone, Debug)]
pub struct TList {
    pixels: Vec<image::Rgba<u8>>,
    locations: Vec<TLocation>,
}

const VERSION: u32 = 2;

impl TList {
    pub fn new() -> Self {
        TList {
            pixels: Vec::new(),
            locations: Vec::new(),
        }
    }

    pub fn add(&mut self, img: &Bitmap<msdfgen::Rgba<f32>>) -> TLocation {
        let width = img.width() as u16;
        let height = img.height() as u16;
        let result = TLocation::new(self.pixels.len() as u32, width, height);
        self.pixels.reserve((width * height) as usize);
        let mtsdf_pixels = img.pixels();
        for y in (0..height).rev() {
            for x in 0..width {
                let pixel = mtsdf_pixels[(x as usize) + (y as usize) * (width as usize)];
                let pixel = image::Rgba([
                    (pixel.r * 255.0) as u8,
                    (pixel.g * 255.0) as u8,
                    (pixel.b * 255.0) as u8,
                    (pixel.a * 255.0) as u8,
                ]);
                self.pixels.push(pixel);
            }
        }
        self.locations.push(result);
        result
    }

    pub fn add_empty(&mut self) -> TLocation {
        let result = TLocation::new(self.pixels.len() as u32, 0, 0);
        self.locations.push(result);
        result
    }

    pub fn locations(&self) -> &[TLocation] {
        &self.locations
    }

    pub fn data(&self) -> &[image::Rgba<u8>] {
        &self.pixels
    }

    pub fn save(&self, dir: &'_ Path) -> anyhow::Result<()> {
        {
            let mut locations_file = dir.to_path_buf();
            locations_file.push("locations.bin");
            let mut fh = OpenOptions::new()
                .write(true)
                .create(true)
                .open(locations_file)
                .context("failed to write to locations.bin")?;
            fh.write_u32_checked(VERSION)?;
            fh.write_u32_checked(self.locations.len().try_into()?)?;
            fh.write_u32_checked(self.pixels.len().try_into()?)?;
            {
                let slice = self.locations.as_slice();
                let slice = unsafe {
                    slice::from_raw_parts(
                        slice.as_ptr().cast::<u8>(),
                        mem::size_of::<TLocation>() * slice.len(),
                    )
                };
                fh.write(slice)?;
            }
            {
                let slice = self.pixels.as_slice();
                let slice = unsafe {
                    slice::from_raw_parts(
                        slice.as_ptr().cast::<u8>(),
                        mem::size_of::<image::Rgba<u8>>() * slice.len(),
                    )
                };
                fh.write(slice)?;
            }
        }

        Ok(())
    }

    pub fn load(dir: &'_ Path) -> anyhow::Result<Self> {
        let mut locations_file = dir.to_path_buf();
        locations_file.push("locations.bin");
        let mut fh = OpenOptions::new()
            .read(true)
            .open(locations_file)
            .context("failed to read locations.bin")?;

        let version = fh.read_u32_checked()?;
        if version != VERSION {
            bail!("version mismatch (expected version {VERSION}; found {version}");
        }

        let num_locations = fh.read_u32_checked()?;
        let num_pixels = fh.read_u32_checked()?;

        let mut locations = vec![TLocation::default(); num_locations as usize];
        {
            let slice = locations.as_mut_slice();
            let slice = unsafe {
                slice::from_raw_parts_mut(
                    slice.as_mut_ptr().cast::<u8>(),
                    mem::size_of::<TLocation>() * slice.len(),
                )
            };
            fh.read_exact(slice)?;
        }

        let mut pixels = vec![image::Rgba::from([0; 4]); num_pixels as usize];
        {
            let slice = pixels.as_mut_slice();
            let slice = unsafe {
                slice::from_raw_parts_mut(
                    slice.as_mut_ptr().cast::<u8>(),
                    mem::size_of::<image::Rgba<u8>>() * slice.len(),
                )
            };
            fh.read_exact(slice)?;
        }

        // Validate the tlist
        for (i, location) in locations.iter().enumerate() {
            let start = location.offset();
            let end = start + (location.width() as u32) * (location.height() as u32);
            if (end as u64) > (pixels.len() as u64) {
                bail!(
                    "location #{i} out of bounds: [{start}, {end}) is not within [0, {})",
                    pixels.len()
                );
            }
        }

        Ok(Self { locations, pixels })
    }
}
