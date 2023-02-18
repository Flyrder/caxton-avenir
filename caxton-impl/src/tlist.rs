use std::io::{Read, Write};
use std::{mem, slice};

use anyhow::{bail, Context};
use flate2::read::ZlibDecoder;
use flate2::write::ZlibEncoder;
use flate2::Compression;
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
    pixels: Vec<u8>,
    locations: Vec<TLocation>,
}

#[derive(Debug)]
pub struct TListPendingEntry<'a> {
    pixels: &'a mut [u8],
}

const VERSION: u32 = 3;

impl TList {
    pub fn new() -> Self {
        TList {
            pixels: Vec::new(),
            locations: Vec::new(),
        }
    }

    pub fn adds(
        &mut self,
        bit_depth: usize,
        sizes: impl Iterator<Item = (u32, u32)>,
    ) -> anyhow::Result<Vec<TListPendingEntry<'_>>> {
        let first_index = self.pixels.len();
        let mut pending_index = first_index;
        let lengths = sizes
            .map(|(w, h)| {
                let w: u16 = w
                    .try_into()
                    .context("width must not be greater than 65535")?;
                let h: u16 = h
                    .try_into()
                    .context("height must not be greater than 65535")?;
                self.locations.push(TLocation::new(
                    (pending_index >> 2)
                        .try_into()
                        .context("offset must not exceed u32::MAX")?,
                    w,
                    h,
                ));
                let pixel_count = bit_depth * (w as usize) * (h as usize);
                let pixel_count = (pixel_count + 3) >> 2 << 2;
                pending_index += pixel_count;
                anyhow::Result::Ok(pixel_count)
            })
            .collect::<anyhow::Result<Vec<_>>>()?;
        let total_length: usize = lengths.iter().copied().sum();
        self.pixels.resize(first_index + total_length, 0);
        let mut result = Vec::new();
        pending_index = first_index;
        let slice_ptr = self.pixels.as_mut_ptr();
        for length in lengths {
            let subslice =
                unsafe { slice::from_raw_parts_mut(slice_ptr.add(pending_index), length) };
            result.push(TListPendingEntry { pixels: subslice });
            pending_index += length;
        }
        Ok(result)
    }

    pub fn locations(&self) -> &[TLocation] {
        &self.locations
    }

    pub fn data(&self) -> &[u8] {
        &self.pixels
    }

    fn save1(&self, fh: &mut impl Write) -> anyhow::Result<()> {
        fh.write_u32_checked(self.locations.len().try_into()?)?;
        fh.write_u32_checked((self.pixels.len() >> 2).try_into()?)?;
        {
            let slice = self.locations.as_slice();
            let slice = unsafe {
                slice::from_raw_parts(
                    slice.as_ptr().cast::<u8>(),
                    mem::size_of::<TLocation>() * slice.len(),
                )
            };
            fh.write_all(slice)?;
        }
        fh.write_all(self.pixels.as_slice())?;

        Ok(())
    }

    pub fn save(tlists: &[TList], fh: &mut impl Write) -> anyhow::Result<()> {
        let mut fh = ZlibEncoder::new(fh, Compression::default());
        fh.write_u32_checked(VERSION)?;
        fh.write_u8_checked(tlists.len().try_into()?)?;
        for tlist in tlists {
            tlist.save1(&mut fh)?;
        }

        Ok(())
    }

    fn load1(fh: &mut impl Read) -> anyhow::Result<Self> {
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

        //
        let mut pixels = vec![0u8; (num_pixels as usize) << 2];
        fh.read_exact(&mut pixels)?;

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

    pub fn load(fh: &mut impl Read, expected_len: usize) -> anyhow::Result<Vec<Self>> {
        let mut fh = ZlibDecoder::new(fh);

        let version = fh.read_u32_checked()?;
        if version != VERSION {
            bail!("version mismatch (expected version {VERSION}; found {version}");
        }
        let len = fh.read_u8_checked()? as usize;
        if len != expected_len {
            bail!("expected {expected_len} elements; got {len} elements");
        }

        (0..len).map(|_| Self::load1(&mut fh)).collect()
    }
}

impl<'a> TListPendingEntry<'a> {
    pub fn put(&mut self, img: &Bitmap<msdfgen::Rgba<f32>>) {
        let width = img.width() as u16;
        let height = img.height() as u16;
        assert_eq!(self.pixels.len(), 4 * (width as usize) * (height as usize));
        let mtsdf_pixels = img.pixels();
        let mut i = 0;
        for y in (0..height).rev() {
            for x in 0..width {
                let pixel = mtsdf_pixels[(x as usize) + (y as usize) * (width as usize)];
                self.pixels[i] = (pixel.r * 255.0) as u8;
                self.pixels[i + 1] = (pixel.g * 255.0) as u8;
                self.pixels[i + 2] = (pixel.b * 255.0) as u8;
                self.pixels[i + 3] = (pixel.a * 255.0) as u8;
                i += 4;
            }
        }
    }
}
