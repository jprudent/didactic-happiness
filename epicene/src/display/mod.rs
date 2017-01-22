use piston::window::WindowSettings;
use piston::event_loop::*;
use piston::input::*;
use glutin_window::GlutinWindow as Window;
use opengl_graphics::{GlGraphics, OpenGL};

#[derive(Clone, Debug, Eq, PartialEq)]
enum Pixel {
    White = 0,
    LightGray = 1,
    DarkGray = 2,
    Black = 3
}

type X = u8;
type Y = u8;

pub struct Tile {
    position: (X, Y),
    pixels: [[Pixel; 8]; 8],
    horizontal_flip: bool,
    vertical_flip: bool,
}

impl Tile {
    fn pixel_at(&self, x: X, y: Y) -> Option<Pixel> {
        let (tile_x, tile_y) = self.position;
        if Tile::between(x, tile_x, tile_x + 7) && Tile::between(y, tile_y, tile_y + 7) {
            let line = &self.pixels[y as usize % 8];
            let pixel = &line[x as usize % 8];
            Some(pixel.clone())
        } else {
            None
        }
    }

    fn between(x: u8, lower: u8, higher: u8) -> bool {
        x >= lower && x <= higher
    }
}

mod test {
    use super::{Tile, Pixel};

    #[test]
    fn should_find_pixel() {
        let tile = Tile {
            vertical_flip: false,
            horizontal_flip: false,
            position: (0, 0),
            pixels: [
                [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                    Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::White, ],
                [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                    Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ],
                [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                    Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ],
                [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                    Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ],
                [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                    Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ],
                [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                    Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ],
                [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                    Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ],
                [Pixel::DarkGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                    Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ]
                ,
            ]
        };

        assert_eq!(tile.pixel_at(0, 7).unwrap(), Pixel::DarkGray);
    }
}

pub struct LcdState {
    line: u8,
    all_tiles: Vec<Tile>
}

pub struct App {
    gl: GlGraphics,
    // OpenGL drawing backend.
    // Rotation for the square.
    lcd_state: LcdState
}

impl App {
    fn render(&mut self, args: &RenderArgs) {
        use graphics::*;

        let pixel_width = args.width as f64 / 160.0;
        let pixel_height = args.height as f64 / 144.0;

        let y = self.lcd_state.line;
        for x in 0..160 {
            for tile in self.lcd_state.all_tiles.iter() {
                if let Some(pixel) = tile.pixel_at(x, y) {
                    let color = match pixel {
                        Pixel::White => [1.0, 1.0, 1.0, 1.0],
                        Pixel::LightGray => [0.3, 0.3, 0.3, 1.0],
                        Pixel::DarkGray => [0.6, 0.6, 0.6, 1.0],
                        Pixel::Black => [0.0, 1.0, 0.0, 1.0],
                    };

                    let rectangle_pixel = rectangle::rectangle_by_corners(0.0, 0.0, pixel_width, pixel_height);

                    self.gl.draw(args.viewport(), |c, gl| {
                        let transform = c.transform.trans(
                            (x as f64 * pixel_width),
                            (y as f64 * pixel_height));
                        rectangle(color, rectangle_pixel, transform, gl);
                    });
                }
            }
        }
    }
}


fn window() {
    let opengl = OpenGL::V3_2;

    // Create an Glutin window.
    let mut window: Window = WindowSettings::new(
        "spinning-square",
        [200, 200]
    )
        .opengl(opengl)
        .exit_on_esc(true)
        .build()
        .unwrap();

    // Create a new game and run it.
    let mut app = App {
        gl: GlGraphics::new(opengl),
        lcd_state: LcdState {
            line: 7,
            all_tiles: vec!(Tile {
                vertical_flip: false,
                horizontal_flip: false,
                position: (56, 0),
                pixels: [
                    [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                        Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ],
                    [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                        Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ],
                    [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                        Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ],
                    [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                        Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ],
                    [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                        Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ],
                    [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                        Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ],
                    [Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray,
                        Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, Pixel::LightGray, ],
                    [Pixel::White, Pixel::White, Pixel::White, Pixel::White,
                        Pixel::White, Pixel::White, Pixel::White, Pixel::White, ],
                ]
            })
        },
    };

    let mut events = window.events();
    while let Some(e) = events.next(&mut window) {
        if let Some(r) = e.render_args() {
            app.render(&r);
        }

        if let Some(u) = e.update_args() {
            //app.update(&u);
        }
    }
}

#[test]
#[allow(dead_code)]
fn f() {
    window()
}