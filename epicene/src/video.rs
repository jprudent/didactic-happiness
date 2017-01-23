use memory::{Ram, MemoryBacked};
use {Address, Word};
use display::{Tile, Pixel};

pub struct VideoRam {
    tile_data: Ram,
    //  8000h-97FFh there are two Tile Pattern Tables at $8000-8FFF and at $8800-97FF
    background_map_1: Ram,
    //  9800h-9BFFh
    background_map_2: Ram,
    // 9C00h-9FFFh
    object_attribute_memory: Ram // FE00-FE9F
}

impl VideoRam {
    pub fn new() -> VideoRam {
        VideoRam {
            tile_data: Ram::new(0x1800, 0x8000),
            background_map_1: Ram::new(0x400, 0x9800),
            background_map_2: Ram::new(0x400, 0x9C00),
            object_attribute_memory: Ram::new(0xA0, 0xFE00)
        }
    }

    fn memory_backend(&self, address: Address) -> &MemoryBacked {
        match address {
            address if in_range(address, 0x8000, 0x97FF) => &self.tile_data,
            address if in_range(address, 0x9800, 0x9BFF) => &self.background_map_1,
            address if in_range(address, 0x9C00, 0x9FFF) => &self.background_map_2,
            address if in_range(address, 0xFE00, 0xFE9F) => &self.object_attribute_memory,
            _ => panic!("Bad memory mapping for video ram at {:04X}", address)
        }
    }

    pub fn build_tiles(&self) -> Vec<Tile> {
        let words = self.object_attribute_memory.raw().borrow();
        let attributes = words.chunks(4);
        let mut ret = vec!();
        for attribute in attributes {
            let y:i16 = attribute[0] as i16 - 16;
            let x:i16 = attribute[1] as i16 - 8;
            if x != -8 {
                println!("position: ({},{})", x, y);
            }
            let pixels = self.fetch_sprite(attribute[2]);
            let horizontal_flip = bit_at(attribute[3], 5) == 1;
            let vertical_flip = bit_at(attribute[3], 6) == 1;
            let tile = Tile {
                pixels: pixels,
                position: (x, y),
                horizontal_flip: horizontal_flip,
                vertical_flip: vertical_flip
            };
            ret.push(tile);
        }
        ret
    }

    fn fetch_sprite(&self, sprite_index: Word) -> [[Pixel; 8]; 8] {
        let words = self.object_attribute_memory.raw().borrow();
        let mut ret = [[Pixel::White; 8]; 8];
        for i in 0..8 {
            let index_1 = ((sprite_index * 4) + (i * 2)) as usize;
            let word_1 = words[index_1];
            let word_2 = words[index_1 + 1];
            for j in 0..8 {
                let b1 = bit_at(word_1, j);
                let b2 = bit_at(word_2, j) << 1;
                let bits = b1 | b2;
                let pixel = Pixel::fromage(bits);
                ret[i as usize][j as usize] = pixel;
            }
        }

        ret
    }
}

fn bit_at(x: Word, position: u32) -> Word {
    x.wrapping_shr(position) & 1
}

impl MemoryBacked for VideoRam {
    fn word_at(&self, address: Address) -> Word {
        self.memory_backend(address).word_at(address)
    }

    fn set_word_at(&self, address: Address, word: Word) {
        self.memory_backend(address).set_word_at(address, word)
    }
}

fn in_range(address: Address, low: Address, high: Address) -> bool {
    address >= low && address <= high
}
