use super::timer::{TimerCommand};
use super::Word;
use std::sync::mpsc::{sync_channel, SyncSender, Receiver};

pub struct DeviceEnd {
    rx: Receiver<TimerCommand>,
    tx: SyncSender<Word>
}

impl DeviceEnd {
    pub fn receive(&self) -> Option<TimerCommand> {
        match self.rx.recv() {
            Ok(r) => Some(r),
            Err(_) => None
        }
    }

    pub fn send(&self, word: Word) {
        self.tx.send(word).unwrap()
    }
}

pub struct MemoryEnd {
    tx: SyncSender<TimerCommand>,
    rx: Receiver<Word>
}

impl MemoryEnd {
    pub fn ask(&self, command: TimerCommand) -> Word {
        self.tx.send(command).unwrap();
        self.rx.recv().unwrap()
    }

    pub fn send(&self, command: TimerCommand) {
        self.tx.send(command).unwrap();
    }
}

pub fn make_bus() -> (DeviceEnd, MemoryEnd) {
    let (command_tx, command_rx) = sync_channel(0);
    let (response_tx, response_rx) = sync_channel(0);
    (DeviceEnd {
        rx: command_rx,
        tx: response_tx
    }, MemoryEnd {
        rx: response_rx,
        tx: command_tx
    })
}
