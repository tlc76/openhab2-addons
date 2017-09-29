# eBUS Binding

The eBUS binding allows you to control your heating system. The eBUS protocol is
used by heating system vendors like Wolf, Vaillant, Kromschröder etc. You can
read temperatures, pump performance, gas consumption etc.

     ┌────────┐                       serial/usb (rs232) ┌──────┐
     │        │  serial (eBUS)  ┌───┐ or ethernet        │ ──── │
     │        │<--------------->│ X │<------------------>│ :    │
     │  ◊◊◊◊  │                 └───┘                    └──────┘
     └────────┘
    Heating Unit             eBUS Adapter              openHAB Server

## Supported Bridge

The bridge connects openHAB to an eBUS interface via serial interface or
Ethernet. You can buy a ready interface or solder your own circuit
(examples: [eBUS Wiki *german*](http://ebus-wiki.org/doku.php/ebus/ebuskonverter)).
A simple read-only interface can be build with an Arduino device.

### List of working devices

- eBus Koppler USB (commercial)
- eBUS Koppler Ethernet (commercial)

You can use Linux tools like ``ser2net`` or ``socat`` to connect your serial
eBUS interface to Ethernet.

### Example

    socat /dev/ttyUSBn,raw,b2400,echo=0 tcp-listen:8000,fork

## Supported Things

The build-in things are listed below.  
_We need your help to support more devices!_ See 
[eBUS Configuration](#ebus-configuration) for more Information.

- **eBUS Standard**  
eBUS Standard commands

- **Vaillant VRC Common**  
Vaillant VRC Common (VRC 430, VRC 470, VRC 90)

- **Vaillant BAI00**  
Vaillant BAI00

- **Wolf CGB-2**  
Condensing gas boiler Wolf CGB-2

- **Wolf BM2**  
Programming unit Wolf BM-2

- **Wolf SM1**  
Solar Module Wolf CGB-2

## Discovery

All devices connected to a eBUS gateway. All required openHAB metadata are 
generated during device discovery. 

## Binding Configuration

You can add up to three custom configuration files. This allows you to add new 
eBUS devices without to update this binding. You must use the URL syntax. 
For more information see [URL](https://en.wikipedia.org/wiki/URL) on wikipedia.

There are several settings for a binding:

- **configurationUrl**  
Define a URL to load external configuration files

- **configurationUrl2**  
Define a second URL to load external configuration files

- **configurationUrl3**  
Define a third URL to load external configuration files

### Examples

    http://www.mydomain.com/files/custom-configuration.json
    file:///etc/openhab/custom-configuration.json

## Bridge Configuration

The bridge is the central communication gateway to your heating system. It is 
mandatory to set the network or serial port parameters.

There are several settings for a bridge:

- **ipAddress**  
Network address or hostname of the eBUS interface

- **port**  
Port of the eBUS interface

- **serialPort**  
Serial port

- **masterAddress**  
Master address of this bridge as HEX, default is ``FF``

## Thing Configuration

There are several settings for a bridge:

- **bridge** (required)  
Select an eBUS bridge

- **slaveAddress** (required)  
Slave address of this node as HEX value like ``FF``

Advanced settings:

- **masterAddress**  
Master address of this node as HEX value like ``FF``. Usually does not have to 
be set. Calculated on the basis of the slave address.

- **filterAcceptMaster**  
Accept telegrams for master address, default is ``false``

- **filterAcceptSlave**  
Accept telegrams for slave address, default is ``true``

- **filterAcceptBroadcasts**  
Accept broadcasts telegrams from master address, default is ``true``

## Channel Groups

A channel group a eBUS command with its values. This can be one or more.

## Channels

There are several settings for a channel:

- **polling** (required)  
Poll a getter command every n seconds from a eBUS slave. All channels of a 
channel group will be refreshed by one polling. Polling is not available on 
broadcast commands.

## Issues

* If receive an error like
  ``java.lang.NoClassDefFoundError: gnu/io/SerialPortEventListener``. You can call
  the command below to install the missing serial library.

```
feature:install openhab-transport-serial
```

* Binding stops working, console shows
  ``Send queue is full! The eBUS service will reset the queue to ensure proper operation.``

This is maybe a hardware fault by your USB/Serial Converter. Specially cheap
adapters with FTDI chips are fakes. You can try to reduce the USB speed on 
Linux, see [here](https://github.com/raspberrypi/linux/issues/1187).

## Logging

If you want to see what's going on in the binding, switch the loglevel to DEBUG
in the Karaf console

```
log:set DEBUG org.openhab.binding.ebus
```

If you want to see even more, switch to TRACE to also see the gateway
request/response data

```
log:set TRACE org.openhab.binding.ebus
```

Set the logging back to normal

```
log:set INFO org.openhab.binding.ebus
```

You can also set the logging for the core library. In that case use
``de.csdev.ebus``

# eBUS Configuration



## Configuration format


### Header block

```javascript
{
    "id":           "device_id",
    "vendor":       "Vendor Name",
    "label":        "A nice label",
    "description":  "A longer description",
    
    "authors":      ["Your author name"],
    "identification": ["FF FF FF FF FF"],
    "properties":   {
                        "key" : "value"
                    },
    "commands":     [
        <command>,
        <command>,
        ...
    ]
}
```

Key            | Required | Description
---            | ---      | ---
id             | x        | The unique short id of this file
vendor         | x        | Then device vendor
label          | x        | The device label
description    | x        | A longer description
authors        |          | Multiple authors of this file as array
identification |          | The device identifications as hex string with five bytes. Multiple values allowed.
properties     |          | Additional key-values
commands       | x        | The array of commands, see command block below

### Command block

```javascript
{
    "label":    "Command label",
    "id":       "groupdId.commandId",
    "command":  "FF FF",
    
    "template": [<template_value>, <template_value>, ...],
    
    <method-block>,
    <method-block>,
    ...
}
```

Key | Required | Description
--- | --- | ---
label | x | A label for this command
id | x | A unique identifier within this file
command | | The two byte hex string for the eBUS command
template | | 
get | | 
set | | 
broadcast | | 

### Method block

```javascript
"set": {
    "command": "FF FF",
    "master": [<value>, <value>, ...]
},

"get": {
    "command": "FF FF",
    "master": [<value>, <value>, ...],
    "slave": [<value>, <value>, ...]
},

"broadcast": {
    "command": "FF FF",
    "master": [<value>, <value>, ...]
}
```

Key       | Required | Description
---       | ---      | ---
set       |          | The command configuration for the setter method
get       |          | The command configuration for the getter method
broadcast |          | The command configuration for the broadcast method
master    |          | The value list for master data, see value block below
slave     |          | The value list for slave data, see value block below
command   |          | The value list for slave data, see value block below

### Value block

```javascript
{
"name":"value_name",
"type": "typeId",
"label": "A nice label",
"format":"%.1f°C",
"min":0,
"max":100,
"factor": 0.1,
"step": 0.5,
"length": 2,
"default": "FF",
"children": [<value>, <value>, ...]
}
```

Key      | Required | Description
---      | ---      | ---
name     | x        | The unique name within this command.<br />Use underscore ``_`` for multiple-word names
type     | x        | The data type of this value
label    |          | The label of this value
format   |          | Formatter for this value,   see String.format
min      |          | A minimal allowed value
max      |          | A maximal allowed value
factor   |          | Multiply the raw value with this factor
step     |          | The allowed step size for this value
length   |          | The length for variable data types like ``bytes``, ``string`` and ``mword``
children |          | sub values for type ``byte``
variant  |          | Currently used by ``datetime``
pow      |          | Currently used by ``mword``
default  |          | A default value, usually as hex string

## Data types

### Standard data types

Key    | Alias | Len | Description
---    | ---   | --- | ---
bcd    |       | 1   | BCD number
bit    |       | 1   | Bit as child of ``byte``
byte   | uchar | 1   | Byte
char   |       | 1   | Char
data1b |       | 1   | DATA1B
data1c |       | 1   | DATA1C
data2b |       | 2   | DATA2B
data2c |       | 2   | DATA2C
int    |       | 2   | Integer
word   | uint  | 2   | Unsigned Word

### Advanced data types

Key      | Alias | Len   | Add. Param              | Description
---      | ---   | ---   | ---                     | ---
bytes    |       | len   | ``length``              | Byte Array, requires ``length``
datetime |       | 3,4,7 | ``variant``             | A DateTime value (variants: ``datetime`` (default), ``date`` and ``time``
kw-crc   |       | 1     |                         | Kromschröder/Wolf CRC, often seen as ``0xCC``
mword    |       | len*2 | ``length``, ``variant`` | Multiple word, requires ``length`` and allows to set ``pow`` (default: 1000)
string   |       | len   | ``length``              | ASCII String, requires ``length``
static   |       | len   | ``default``             | A static byte array with the value of ``default``
template |       |       | ``name``                | Adds the value with the given ``name`` from the template block
template-block | |       |                         | Adds the complete template block on this position

## Example
```javascript
{
    "id":           "common",
    "vendor":       "eBUS Group",
    "label":        "eBUS Standard",
    "description":  "eBUS Group Standard commands",
    
    "authors":      ["Christian Sowada, opensource@cs-dev.de"],
    "identification": [],
    
    "commands":
    [
        {
            "label":    "Outside temperature",
            "id":       "burner.temp_outside",
            "command":  "03 01",
    
            "broadcast": {
                "master": [
                    {"name":"temp_outside", "type": "word", "label": "Outside temperature", "factor": 0.1, "format":"%.1f°C"}
                ]
            }
        },
    
        {
            "label":    "Operational Data of Burner Control unit to Room Control Unit - Block 1",
            "id":       "auto_stroker.op_data_bc2tc_b1",
            "command":  "05 03",

            "get": {
                "master": [
                    {"name":"block_number", "type": "static", "default": "01"},
                    {"name":"status_auto_stroker", "type": "byte", "label": "Status indication"},
                    {"type": "byte", "children": [
                        {"name":"state_air_pressure", "type": "bit", "label": "Air pressure monitor"},
                        {"name":"state_gas_pressure", "type": "bit", "label": "Gas pressure monitor"},
                        {"name":"state_water_flow", "type": "bit", "label": "Water flow"},
                        {"name":"state_flame", "type": "bit", "label": "Flame"},
                        {"name":"state_valve1", "type": "bit", "label": "Valve1"},
                        {"name":"state_valve2", "type": "bit", "label": "Valve2"},
                        {"name":"state_pump", "type": "bit", "label": "State heating pump"},
                        {"name":"state_alarm", "type": "bit", "label": "Alarm"}
                    ]},                
                    {"name":"performance_burner", "type": "uchar", "label": "Setting degree between min. and max. boiler performance (%)","min":0.0, "max":100.0,  "format":"%d%%"},
                    {"name":"temp_boiler", "type": "data1c", "label": "Boiler temperature (°C)", "min":0, "max":100, "format":"%.1f°C"},
                    {"name":"temp_return", "type": "uchar", "label": "Return temperature (°C)", "min":0, "max":100, "format":"%d°C"},
                    {"name":"temp_dhw", "type": "uchar", "label": "DHW temperature (°C)", "min":0, "max":100, "format":"%d°C"},
                    {"name":"temp_outside", "type": "char", "label": "Outside temperature (°C)", "min":-30, "max":50, "format":"%d°C"}
                ]
            }
        },
        ...
        
    ]
}
```
