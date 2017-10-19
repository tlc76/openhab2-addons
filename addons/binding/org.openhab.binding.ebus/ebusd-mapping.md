ebusd type | eBUS binding type | add. params | xxxx
--- | --- | --- | --- 
IGN | byte | without name
STR | string
NTS |
HEX | char
BDA | date | | day first, excluding weekday
BDA:3 | date | variant: short | day first, including weekday, Sunday=0x06
HDA   | date | variant: hex | dd.mm.yyyy | day first, including weekday, Sunday=0x07
HDA:3 | date | variant: hex_short | day first, excluding weekday
DAY   | date | variant: days | days since 01.01.1900
BTI | time | | seconds first
BTM | time | variant: short | minute first
VTI | time | variant: hex | seconds first
VTM | time | variant: hex_short | minute first
HTI | time | variant: hex, reverseByteOrder: true | hours first
HTM | time | variant: hex_short, reverseByteOrder: true | hours first
MIN | time | variant: min | minutes since midnight
TTM |
TTQ |
BDY |
HDY |
BCD | bcd | | BCD value
x BCD:2 | bcd | length: 2 | BCD value
x BCD:3 | bcd | length: 3 | BCD value
x BCD:4 | bcd | length: 4 | BCD value
x HCD |||each BCD byte converted to hex
x HCD:1 |||each BCD byte converted to hex
x HCD:2 |||each BCD byte converted to hex
x HCD:3 |||each BCD byte converted to hex
x PIN | bcd | length: 2 | BCD value
UCH | byte/(uchar) | | unsigned char, primary type 
SCH | char | | signed char, primary type 
D1C | data1c | | same as ``char``
D2B | data2b | | divisor 2
D2C | data2c | | divisor 256
FLT | int | factor: 0.001
FLR | int | factor: 0.001, reverseByteOrder: true
EXP | |
EXR | |
UIN | word/(uint) |
UIR | word/(uint) | reverseByteOrder: true
SIN | int |
SIR | int | reverseByteOrder: true
x U3N | word/(uint) | length: 3 | low byte first
x U3R | word/(uint) | length: 3, reverseByteOrder: true | high byte first
x S3N | int | length: 3 | low byte first
x S3R | int | length: 3, reverseByteOrder: true | high byte first
x ULG | word/(uint) | length: 4 | low byte first
x ULR | word/(uint) | length: 4, reverseByteOrder: true | high byte first
x SLG | int | length: 4 | low byte first
x SLR | int | length: 4, reverseByteOrder: true | high byte first
DI0-7 | byte | children of ``bit``
