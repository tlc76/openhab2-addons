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


## Installation

### ZIP file

**Deprecated:** To install this binding copy the contents of the zip file into the `openhab\addon`
directory. The zip file contains the individual bundles of the binding.

### KAR file

To install this binding copy the kar file into the  `openhab\addon` directory.
The kar file is an archive that contains everything.

### Marketplace

Install the binding over the market place 
[link](https://marketplace.eclipse.org/content/ebus-20-binding).

### Serial driver (optional)

If you use a serial adapter, you maybe need to install the openhab serial 
driver. Enter the command below in your openhab console. To get you serial 
port running with Linux you also need to set start parameters. See openHAB 
documentation - 
[Privileges for Common Peripherals](http://docs.openhab.org/installation/linux.html#privileges-for-common-peripherals)

```
feature:install openhab-transport-serial
```


## Supported Things

### eBUS Interfaces

The binding connects openHAB to an eBUS interface via serial interface or
Ethernet. You can buy a ready interface or solder your own circuit
(examples: [eBUS Wiki *german*](http://ebus-wiki.org/doku.php/ebus/ebuskonverter)).
A simple read-only interface can be build with an Arduino device.

Keep in mind, that most interfaces need to be adjusted before first use.

#### List of working devices

- eBus Koppler USB (commercial)
- eBUS Koppler Ethernet (commercial)

You can use Linux tools like `ser2net` or `socat` to connect your serial
eBUS interface over Ethernet. See example at the end of this file.

### Heating Devices

Your heating system must support the eBUS protocol. Connect your eBUS interface 
to your heating system. A heating system normally consists of several components, 
like Burners, mixers and solar modules. Please check your manual.

The build-in things are listed below.  
_We need your help to support more devices!_ See 
[eBUS Configuration](#create-your-own-ebus-configuration-files) for more Information.

- **eBUS Standard**  
eBUS Standard commands

- **Vaillant VRC Common**  
Vaillant VRC Common (VRC 430, VRC 470, VRC 90, VRC 700)

- **Vaillant BAI00**  
Vaillant BAI00

- **Wolf CGB-2**  
Condensing gas boiler Wolf CGB-2

- **Wolf BM2**  
Programming unit Wolf BM-2

- **Wolf SM1**  
Solar Module Wolf CGB-2

- **Wolf MM**  
Mixer Module Wolf



## Discovery

This binding is able to resolve many devices automatically. It's listening for 
new devices on the bus and try to identify the device. If the device ID is 
known, a corresponding thing is added to the inbox. In any case, an eBUS 
standard thing is added for each detected device.


## Binding Configuration

You can add up to three custom configuration files or one bundle file. This 
allows you to add new eBUS devices without updating this binding. The built-in 
configurations are always loaded first, the own files are loaded afterwards. 
These can also overwrite already loaded commands. You must use the URL syntax. 
For more information see [URL](https://en.wikipedia.org/wiki/URL) on wikipedia.

There are several settings for a binding:

- **Configuration URL** _(configurationUrl)_  
Define a URL to load external configuration files

- **Configuration URL** _(configurationUrl2)_  
Define a second URL to load external configuration files

- **Configuration URL** _(configurationUrl3)_  
Define a third URL to load external configuration files

- **Configuration Bundle URL** _(configurationBundleUrl)_  
Define a bundle URL to load a set of configurations at once.


### Example URLs

    http://www.mydomain.com/files/custom-configuration.json
    file:///etc/openhab/custom-configuration.json


## Bridge Configuration

The bridge is the central communication gateway to your heating system. It is 
mandatory to set the network or serial port parameters.

There are several settings for a bridge:

- **Network Address** _(ipAddress)_  
Network address or hostname of the eBUS interface

- **Network Port** _(port)_  
Port of the eBUS interface

- **Serial Port** _(serialPort)_  
Serial port

- **Master Address** _(masterAddress)_  
Master address of this bridge as HEX, default is `FF`

- **Advanced Logging** _(advancedLogging)_  
Enable more logging for this bridge, default is `false`


## Thing Configuration

There are several settings for a thing:

- **Bridge** _(bridge)_ (required)  
Select an eBUS bridge

- **Slave Address** _(slaveAddress)_ (required)  
Slave address of this node as HEX value like `FF`


**Advanced settings:**

- **eBUS Master Address** _(masterAddress)_  
Master address of this node as HEX value like `FF`. In general, this value 
must not be set, since this value is calculated on the basis of the slave address.

- **Accept for master address** _(filterAcceptMaster)_  
Accept telegrams for master address, default is `false`

- **Accept for slave address** _(filterAcceptSlave)_  
Accept telegrams for slave address, default is `true`

- **Accept broadcasts** _(filterAcceptBroadcasts)_  
Accept broadcasts telegrams from master address, default is `true`

- **Polling all channels** _(polling)_  
Poll all getter channels every n seconds from an eBUS slave. The binding starts
every eBUS command with a random delay to scatter the bus access.


## Channel Configuration

Polling can be set for all getter channels. The polling applies to all channels in 
a group. Thus, the value must only be set for one channel.

There are only one settings for a channel:

- **Polling** _(polling)_ 
Poll a getter channel every n seconds from a eBUS slave. All channels of a 
channel group will be refreshed by one polling. Polling is not available on 
broadcast commands.


## Channels

Due to the long and dynamic channels, there is no list here.


## Full Example

It is also possible to set up the configuration by text instead of PaperUI. Due to 
the dynamic channels the configuration is not as comfortable as with PaperUI. The 
problem is finding the right IDs.
You should first setup the configuration via PaperUI. From there you can copy 
the information for things and channels.

You can get the channel type by copying the channel id from PaperUI and replace the hash ``#`` character with and underscore ``_`` character.

**.thing file**

```java
Bridge ebus:bridge:home1 "eBUS Bridge (serial)" @ "Home" [ serialPort="/dev/ttyUSB1", masterAddress="FF", advancedLogging=true ] {
  Thing std 08 "My eBUS Standard at address 08" [ slaveAddress="08" ]
  Thing vrc430 15 "My VRC430 at address 15" [ slaveAddress="15" ] {
    Channels:
      Type vrc430_heating_program-heating-circuit_program : vrc430_heating_program-heating-circuit#program [ polling = 60 ]
  }
}
```

```java
Bridge ebus:bridge:home2 "eBUS Bridge2" [ ipAddress="10.0.0.2", port=80 ] {
    ...
}
```

**.items file**

```java
Number Heating_HC_Program "Heating Program [%s]" (Heating) { channel="ebus:vrc430:home1:15:vrc430_heating_program-heating-circuit#program" }
```


## Console Commands

This binding also brings some useful console commands to get more information from
the configuration.

    smarthome:ebus list                                    lists all eBUS devices
    smarthome:ebus send "<ebus telegram>" [<bridgeUID>]    sends a raw hex telegram to an eBUS bridge or if not set to first bridge
    smarthome:ebus devices [<bridgeUID>]                   lists all devices connect to an eBUS bridge or list only a specific bridge
    smarthome:ebus resolve "<ebus telegram>"               resolves and analyze a telegram
    smarthome:ebus reload                                  reload all defined json configuration files
    smarthome:ebus update                                  update all things to newest json configuration files


## Issues

* If receive an error like
  `java.lang.NoClassDefFoundError: gnu/io/SerialPortEventListener`. You can call
  the command below to install the missing serial library.

```
feature:install openhab-transport-serial
```

* Binding stops working, console shows
  `Send queue is full! The eBUS service will reset the queue to ensure proper operation.`

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


#### Socat Example

    socat /dev/ttyUSBn,raw,b2400,echo=0 tcp-listen:8000,fork


## Create your own eBUS configuration files

You can create or customize your own configuration files. The configuration files are json text files. You find the documentation
in the link list. You should inspect the included configuration files before.

* [eBUS Configuration Format](https://github.com/csowada/ebus/blob/master/de.csdev.ebus/doc/configuration.md)
* [eBUS Configuration ebusd Mapping](https://github.com/csowada/ebus/blob/master/de.csdev.ebus/doc/ebusd-mapping.md)
* [eBUS Core Lib included configurations](https://github.com/csowada/ebus/tree/master/de.csdev.ebus/src/main/resources/commands)