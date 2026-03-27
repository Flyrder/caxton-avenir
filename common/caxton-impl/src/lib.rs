extern crate core;
extern crate nalgebra as na;

pub mod cfont;
pub mod error;
pub mod extrude;
pub mod ffm;
pub mod font;
pub mod shape;
pub mod tlist;

fn to_i16_clamped(x: f32) -> i16 {
    x.clamp(i16::MIN as f32, i16::MAX as f32) as i16
}

#[cfg(test)]
mod tests {
    use crate::extrude::Kernel;

    #[test]
    fn test_kernel_construction() {
        let kernel = Kernel::with_radius(0.5);
        assert_eq!(&*kernel.0, &[1, 0]);
        let kernel = Kernel::with_radius(1.0);
        assert_eq!(&*kernel.0, &[1, 1]);
        let kernel = Kernel::with_radius(1.5);
        assert_eq!(&*kernel.0, &[2, 1, 0]);
        let kernel = Kernel::with_radius(2.0);
        assert_eq!(&*kernel.0, &[2, 2, 1]);
    }

    #[test]
    fn test_kernel_construction_doesnt_panic() {
        for i in 0..100 {
            let r = (i as f64) / 10.0;
            eprintln!("Trying to construct kernel for r = {r}");
            let kernel = Kernel::with_radius(r);
            eprintln!("Kernel: {kernel:?}");
        }
    }

    #[test]
    fn test_convolve() {
        let kernel = Kernel::with_radius(1.0);
        let orig = [
            [1, 0, 0, 0, 0],
            [0, 0, 0, 0, 0],
            [0, 0, 5, 0, 0],
            [0, 0, 0, 0, 0],
            [0, 0, 0, 0, 9],
        ];
        let expected = [
            [1, 1, 0, 0, 0],
            [1, 5, 5, 5, 0],
            [0, 5, 5, 5, 0],
            [0, 5, 5, 9, 9],
            [0, 0, 0, 9, 9],
        ];
        let orig = orig.as_flattened();
        let mut actual = vec![0; orig.len()];
        kernel.convolve(orig, &mut actual, 5, 5);
        assert_eq!(expected.as_flattened(), actual)
    }
}
