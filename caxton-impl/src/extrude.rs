use std::borrow::BorrowMut;
use std::collections::btree_map::Entry;
use std::collections::BTreeMap;

// Stupid CS101-level code to generate thicker versions of glyphs for
// drawing outlined text (such as on glow ink signs)

#[derive(Debug, Clone)]
pub struct Kernel(pub Box<[u32]>);

impl Kernel {
    pub fn with_radius(r: f64) -> Self {
        let rint = r.round() as u32;
        let result: Vec<u32> = (0..=rint)
            .map(|y| {
                if y == 0 {
                    rint
                } else {
                    let yp = y as f64 - 0.5;
                    (r * r - yp * yp).sqrt().round() as u32
                }
            })
            .collect();
        // Assert that the result is symmetric about the 45° diagonals
        for (i, w) in result.iter().copied().enumerate() {
            // (w, i) is on the boundary, so (i, w) should be too
            let lb = result.get(w as usize + 1).map(|x| *x as usize);
            let ub = result[w as usize] as usize;
            assert!(
                lb.map_or(true, |lb| lb < i) && i <= ub,
                "kernel {result:?} for radius {r} is not symmetric about the 45° diagonals: {i} is not in ({lb:?}, {ub}]",
            );
        }
        Kernel(result.into_boxed_slice())
    }

    pub fn convolve(&self, src: &[u8], dest: &mut [u8], width: usize, height: usize) {
        assert_eq!(src.len(), width * height);
        assert_eq!(dest.len(), width * height);
        if width == 0 || height == 0 {
            return;
        }
        // Compute the histogram for the point (0, 0)
        let mut histogram = Histogram::default();
        for (dy, w) in self.0.iter().copied().enumerate() {
            if dy >= height {
                break;
            }
            for dx in 0..=w {
                if dx as usize >= width {
                    break;
                }
                histogram.increment(src[(dx as usize) + dy * width]);
            }
        }
        let mut y = 0;
        loop {
            dest[y * width] = histogram.max_brightness();
            // Go forward
            for x in 0..width - 1 {
                for (dy, w) in self.0.iter().copied().enumerate() {
                    let x_right = x + 1 + (w as usize);
                    if x_right < width {
                        if y >= dy {
                            histogram.increment(src[(x + 1 + w as usize) + (y - dy) * width]);
                        }
                        if dy != 0 && y + dy < height {
                            histogram.increment(src[(x + 1 + w as usize) + (y + dy) * width]);
                        }
                    }
                    if x >= w as usize {
                        if y >= dy {
                            histogram.decrement(src[(x - w as usize) + (y - dy) * width]);
                        }
                        if dy != 0 && y + dy < height {
                            histogram.decrement(src[(x - w as usize) + (y + dy) * width]);
                        }
                    }
                }
                dest[x + 1 + y * width] = histogram.max_brightness();
            }
            y += 1;
            if y >= height {
                break;
            }
            // Move the kernel one space down (this assumes that it is symmetric
            // about the 45° diagonals)
            for (dx, h) in self.0.iter().copied().enumerate() {
                if dx < width {
                    if (y + 1 + h as usize) < height {
                        histogram.increment(src[width - 1 - dx + (y + 1 + h as usize) * width]);
                    }
                    if y >= h as usize {
                        histogram.decrement(src[width - 1 - dx + (y - h as usize) * width]);
                    }
                }
            }
            dest[(width - 1) + y * width] = histogram.max_brightness();
            // Go backward
            for x in (0..width - 1).rev() {
                for (dy, w) in self.0.iter().copied().enumerate() {
                    if x >= w as usize {
                        if y >= dy {
                            histogram.increment(src[(x - w as usize) + (y - dy) * width]);
                        }
                        if dy != 0 && y + dy < height {
                            histogram.increment(src[(x - w as usize) + (y + dy) * width]);
                        }
                    }
                    let x_right = x + 1 + (w as usize);
                    if x_right < width {
                        if y >= dy {
                            histogram.decrement(src[(x + 1 + w as usize) + (y - dy) * width]);
                        }
                        if dy != 0 && y + dy < height {
                            histogram.decrement(src[(x + 1 + w as usize) + (y + dy) * width]);
                        }
                    }
                }
                dest[x + y * width] = histogram.max_brightness();
            }
            y += 1;
            if y >= height {
                break;
            }
            // Move the kernel one space down (this assumes that it is symmetric
            // about the 45° diagonals)
            for (dx, h) in self.0.iter().copied().enumerate() {
                if dx < width {
                    if (y + 1 + h as usize) < height {
                        histogram.increment(src[dx + (y + 1 + h as usize) * width]);
                    }
                    if y >= h as usize {
                        histogram.decrement(src[dx + (y - h as usize) * width]);
                    }
                }
            }
        }
    }
}

#[derive(Clone, Debug, Default)]
struct Histogram(BTreeMap<u8, usize>);

impl Histogram {
    fn increment(&mut self, brightness: u8) {
        *self.0.entry(brightness).or_insert(0) += 1;
    }

    fn decrement(&mut self, brightness: u8) {
        if let Entry::Occupied(mut e) = self.0.entry(brightness) {
            *e.get_mut() -= 1;
            if *e.get() == 0 {
                e.remove();
            }
        }
    }

    fn max_brightness(&self) -> u8 {
        match self.0.last_key_value() {
            None => 0,
            Some((k, _v)) => *k,
        }
    }
}
