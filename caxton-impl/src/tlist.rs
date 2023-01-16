use msdfgen::Bitmap;

#[repr(C)]
pub struct TLocation {
    pub offset: u32,
    pub length: u32,
}

pub const INVALID: TLocation = TLocation {
    offset: u32::MAX,
    length: u32::MAX,
};

pub struct TList {
    pixels: Vec<image::Rgba<u8>>,
    locations: Vec<TLocation>,
}

impl TList {
    pub fn new() -> Self {
        TList {
            pixels: Vec::new(),
            locations: Vec::new(),
        }
    }

    pub fn add(&mut self, img: &Bitmap<msdfgen::Rgba<f32>>) -> TLocation {
        let width = img.width();
        let height = img.height();
        let result = TLocation {
            offset: self.pixels.len() as u32,
            length: width * height,
        };
        self.pixels.reserve(result.length as usize);
        let mtsdf_pixels = img.pixels();
        for x in 0..width {
            for y in 0..height {
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
        result
    }

    pub fn locations(&self) -> &[TLocation] {
        &self.locations
    }
}
