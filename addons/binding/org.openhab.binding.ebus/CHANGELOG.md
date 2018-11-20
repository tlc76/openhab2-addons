## Release Candidate 3 (2018-11-20)

Changes:

  - Use openHAB builder pattern
  - Cleanup code, use more constants
  - Lower log level for some debug messages
  - Use openHAB QuantityType
  
Bugfixes:

  - Fix issue if "°C" is used in configuration label
  
## Release Candidate 2 (2018-11-03)

Features:

  - Add additional serial port driver JSerialComm
  - New option in bridge configuration to choose serial port driver
  - Update eBUS library to version 0.9.19
  
Bugfixes:

  - Fix spaces in collection id and NPE if label is null

## Release Candidate 1 (2018-09-30)

Features:

  - Add DS Annotations for service configuration
  
Bugfixes:

  - Add new ChannelGroupTypeProvider to internal provider to fix broken Alpha 23
  
## Alpha 0.0.23 (2018-09-24)

Bugfixes:

  - Fix for 2.4 milestone release, breaking changes in ESH
  
## Alpha 0.0.22 (2018-07-10)

Features:

  - Update ebus core lib and add new exceptions
  
Bugfixes:

  - Fix NPE - Use Apache BeanUtils for configuration

## Alpha 0.0.21 (2018-06-24)

Features:

  - Check Thing configuration on initialize and not set to ``ONLINE``
  
Bugfixes:

  - Polling changes on Thing level are updated immediately

  
## Alpha 0.0.20 (2018-06-23)

Features:

  - Optimize channel polling to react on un/link channels
  - Add new openhab ``Units Of Measurement`` feature for temperature values

Bugfixes:

  - Fix polling regression from previous release
  
## Alpha 0.0.19 (2018-05-23)

Features:

  - Update binding to openHAB 2.3, it is not compatible to a lower version!
  
## Alpha 0.0.18 (2017-05-07)

Features:

  - Update README.MD
  - Add a new console command ``smarthome:ebus channels``
  
## Alpha 0.0.17 (2017-12-04)

...
  
## Alpha 0.0.16 (2017-12-04)

...
  
## Alpha 0.0.15 (2017-12-04)

Features:

  - Update to eBUS library 0.0.15, [github](https://github.com/csowada/ebus/tree/0.0.15)
  
## Alpha 0.0.14 (2017-11-20)

Features:

  - Update to eBUS library 0.0.14, [github](https://github.com/csowada/ebus/tree/0.0.14)
  - Add advance logging to eBUS binding configuration
  - Add configuration bundle URL to eBUS binding configuration
  - Add polling per thing
  
## Alpha 0.0.13 (2017-11-02)

Features:

  - Update to eBUS library 0.0.13, [github](https://github.com/csowada/ebus/tree/0.0.13)
  - Restructure configuration loading, add listener for updates
  
Bugfixes:

  - Fix mapping for EBusTypeDate and EBusTypeTime to openHAB DateTime
  - Update console commands incl. help
  - Fix discovered thing ids
 
## Alpha 0.0.12 (2017-10-22)

Features:

  - Update to eBUS library 0.0.12, [github](https://github.com/csowada/ebus/tree/0.0.12)
  - Smaller changes
  
Bugfixes:

  - Change item type from number to string if options are used
  
## Alpha 0.0.11 (2017-10-15)

Features:

  - Update to eBUS library 0.0.11, [github](https://github.com/csowada/ebus/tree/0.0.11)
  - Add new command ``smarthome:ebus devices`` to openHAB2 console
  - Add new logger for unresolved telegrams ``org.openhab.ebus-ext``