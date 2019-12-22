/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.ebus.internal;

import static org.openhab.binding.ebus.internal.EBusBindingConstants.*;

import java.math.BigDecimal;

/**
 * The {@link EBusConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusConfiguration {

    public String masterAddress;

    public String slaveAddress;

    // public String polling;

    public String serialPort;

    public String ipAddress;

    public BigDecimal port;

    public String raw;

    public String ebusd;

    public String networkDriver;

    public String serialPortDriver;

    public Boolean advancedLogging;

    public String configurationUrl;

    public String configurationUrl1;

    public String configurationUrl2;

    public String configurationBundleUrl;

    public String getMasterAddress() {
        return masterAddress;
    }

    public String getSlaveAddress() {
        return slaveAddress;
    }

    public String getSerialPort() {
        return serialPort;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public BigDecimal getPort() {
        return port;
    }

    public String getRaw() {
        return raw;
    }

    public String getEbusd() {
        return ebusd;
    }

    public String getNetworkDriver() {
        return networkDriver == null ? DRIVER_RAW : networkDriver;
    }

    public String getSerialPortDriver() {
        return serialPortDriver == null ? DRIVER_NRJAVASERIAL : serialPortDriver;
    }

    public Boolean getAdvancedLogging() {
        return advancedLogging;
    }

    public String getConfigurationUrl() {
        return configurationUrl;
    }

    public String getConfigurationUrl1() {
        return configurationUrl1;
    }

    public String getConfigurationUrl2() {
        return configurationUrl2;
    }

    public String getConfigurationBundleUrl() {
        return configurationBundleUrl;
    }

}
