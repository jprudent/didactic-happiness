use memory::{Ram, MemoryBacked};
use {Address, Word};

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

    fn memory_backend(& self, address: Address) -> & MemoryBacked {
        match address {
            address if in_range(address, 0x8000, 0x97FF) => &self.tile_data,
            address if in_range(address, 0x9800, 0x9BFF) => &self.background_map_1,
            address if in_range(address, 0x9C00, 0x9FFF) => &self.background_map_2,
            address if in_range(address, 0xFE00, 0xFE9F) => &self.object_attribute_memory,
            _ => panic!("Bad memory mapping for video ram at {:04X}", address)
        }
    }
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
