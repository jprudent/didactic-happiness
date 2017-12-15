(ns repicene.address-alias)

(def serial-transfer-data
  "SB - Serial transfer data (R/W)
   8 Bits of data to be read/written"
  0xFF01)

(def serial-transfer-control
  "SC - Serial Transfer Control (R/W)
   Bit 7 - Transfer Start Flag (0=No Transfer, 1=Start)
   Bit 1 - Clock Speed (0=Normal, 1=Fast) ** CGB Mode Only **
   Bit 0 - Shift Clock (0=External Clock, 1=Internal Clock
   The clock signal specifies the rate at which the eight data bits in SB
   (FF01) are transferred. When the gameboy is communicating with another
   gameboy (or other computer) then either one must supply internal clock,
   and the other one must use external clock."
  0xFF02)