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

You can use Linux tools like `ser2net` or `socat` to connect your serial
eBUS interface to Ethernet.

#### Example

    socat /dev/ttyUSBn,raw,b2400,echo=0 tcp-listen:8000,fork

## Installtion

To install this binding put the downloaded `org.openhab.binding.ebus-2.x.x-SNAPSHOT.jar` file into `openhab\addon` folder or install the binding over the market place [link](https://marketplace.eclipse.org/content/ebus-20-binding).

If you use a serial apdater, you maybe need to install the openhab serial driver. Enter the commadn below in your openhab console. To get you serial port running with Linux you also need to set start parameters. See openHAB documentation - [Privileges for Common Peripherals](http://docs.openhab.org/installation/linux.html#privileges-for-common-peripherals)

```
feature:install openhab-transport-serial
```

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

- **Configuration URL** _(configurationUrl)_  
Define a URL to load external configuration files

- **Configuration URL** _(configurationUrl2)_  
Define a second URL to load external configuration files

- **Configuration URL** _(configurationUrl3)_  
Define a third URL to load external configuration files

- **Configuration Bundle URL** _(configurationBundleUrl)_  
Define a bundle URL to load a set configurations at once.


### Examples

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

- **masterAddress** _(masterAddress)_  
Master address of this bridge as HEX, default is `FF`

## Thing Configuration

There are several settings for a bridge:

- **Bridge** _(bridge)_ (required)  
Select an eBUS bridge

- **Slave Address** _(slaveAddress)_ (required)  
Slave address of this node as HEX value like `FF`

Advanced settings:

- **eBUS Master Address** _(masterAddress)_  
Master address of this node as HEX value like `FF`. Usually does not have to 
be set. Calculated on the basis of the slave address.

- **Accept for master address** _(filterAcceptMaster)_  
Accept telegrams for master address, default is `false`

- **Accept for slave address** _(filterAcceptSlave)_  
Accept telegrams for slave address, default is `true`

- **Accept broadcasts** _(filterAcceptBroadcasts)_  
Accept broadcasts telegrams from master address, default is `true`

- **Polling all channels** _(polling)_  
Poll all getter commands every n seconds from a eBUS slave. The binding starts
every command with a random delay to scatter the bus access.


## Channel Groups

A channel group a eBUS command with its values. This can be one or more.

## Channels

There are several settings for a channel:

- **Polling** _(polling)_ (required)  
Poll a getter command every n seconds from a eBUS slave. All channels of a 
channel group will be refreshed by one polling. Polling is not available on 
broadcast commands.

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

## eBUS Configuration

You can create or customize your own configuration files. The configuration files are json text files. You find the documentation
in the link list. You should inspect the included configuration files before.

* [eBUS Configuration Format](https://github.com/csowada/ebus/blob/master/de.csdev.ebus/doc/configuration.md)
* [eBUS Configuration ebusd Mapping](https://github.com/csowada/ebus/blob/master/de.csdev.ebus/doc/ebusd-mapping.md)
* [eBUS Core Lib included configurations](https://github.com/csowada/ebus/tree/master/de.csdev.ebus/src/main/resources/commands)