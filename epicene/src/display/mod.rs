use piston::window::WindowSettings;
use piston::event_loop::*;
use piston::input::*;
use glutin_window::GlutinWindow as Window;
use opengl_graphics::{GlGraphics, OpenGL};
use std::thread;
use std::sync::mpsc::Receiver;

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum Pixel {
    White = 0,
    LightGray = 1,
    DarkGray = 2,
    Black = 3
}

type X = u8;
type Y = u8;

pub struct Tile {
    pub position: (X, Y),
    pub pixels: [[Pixel; 8]; 8],
    pub horizontal_flip: bool,
    pub vertical_flip: bool,
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
    pub line: u8,
    pub all_tiles: Vec<Tile>
}

pub struct Screen {
    // OpenGL drawing backend.
    // Rotation for the square.
    pub lcd_state: LcdState
}

impl Screen {
    fn empty() -> Screen {
        Screen {
            lcd_state: LcdState {
                line: 0,
                all_tiles: vec!()
            }
        }
    }
    fn render(&mut self, args: &RenderArgs, gl: &mut GlGraphics) {
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

                    println!("({},{}) color {:?} {} {}", x, y, color, pixel_width, pixel_height);

                    use graphics::*;
                    let rectangle_pixel = rectangle::rectangle_by_corners(0.0, 0.0, pixel_width, pixel_height);

                    gl.draw(args.viewport(), |c, gl| {
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

pub struct Display {
    screen_chan: Receiver<Screen>,
}

impl Display {
    pub fn new(screen_chan: Receiver<Screen>) -> Display {
        Display {
            screen_chan: screen_chan,
        }
    }

    pub fn start(mut self) {
        thread::spawn(move || {
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

            let mut gl = GlGraphics::new(opengl);

            let mut events = window.events();
            while let Some(e) = events.next(&mut window) {
                if let Some(r) = e.render_args() {
                    self.get_screen().render(&r, &mut gl);
                }
            }
        });
    }

    fn get_screen(&mut self) -> Screen {
        self.screen_chan.recv().unwrap()
    }
}

//#[test]
#[allow(dead_code)]
fn f() {
    use std::sync::mpsc::channel;
    use std::thread::sleep_ms;
    let (tx, rx) = channel();
    let mut display = Display::new(rx);
    display.start();
    for i in 0..500 {
        if i % 2 == 0 {
            tx.send(Screen {
                lcd_state: LcdState {
                    line: 1,
                    all_tiles: vec!(Tile {
                        vertical_flip: false,
                        horizontal_flip: false,
                        position: (0, 0),
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
            }).unwrap();
        } else {
            tx.send(Screen {
                lcd_state: LcdState {
                    line: 1,
                    all_tiles: vec!(Tile {
                        vertical_flip: false,
                        horizontal_flip: false,
                        position: (0, 0),
                        pixels: [
                            [Pixel::White, Pixel::White, Pixel::White, Pixel::White,
                                Pixel::White, Pixel::White, Pixel::White, Pixel::White, ],
                            [Pixel::White, Pixel::White, Pixel::White, Pixel::White,
                                Pixel::White, Pixel::White, Pixel::White, Pixel::White, ],
                            [Pixel::White, Pixel::White, Pixel::White, Pixel::White,
                                Pixel::White, Pixel::White, Pixel::White, Pixel::White, ],
                            [Pixel::White, Pixel::White, Pixel::White, Pixel::White,
                                Pixel::White, Pixel::White, Pixel::White, Pixel::White, ],
                            [Pixel::White, Pixel::White, Pixel::White, Pixel::White,
                                Pixel::White, Pixel::White, Pixel::White, Pixel::White, ],
                            [Pixel::White, Pixel::White, Pixel::White, Pixel::White,
                                Pixel::White, Pixel::White, Pixel::White, Pixel::White, ],
                            [Pixel::White, Pixel::White, Pixel::White, Pixel::White,
                                Pixel::White, Pixel::White, Pixel::White, Pixel::White, ],
                            [Pixel::White, Pixel::White, Pixel::White, Pixel::White,
                                Pixel::White, Pixel::White, Pixel::White, Pixel::White, ],
                        ]
                    })
                },
            }).unwrap();
        }
        sleep_ms(500);
    }
}