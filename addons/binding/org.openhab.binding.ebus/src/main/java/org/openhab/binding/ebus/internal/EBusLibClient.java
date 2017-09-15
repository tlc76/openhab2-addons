/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.ebus.EBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.client.EBusClientConfiguration;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.datatypes.EBusTypeException;
import de.csdev.ebus.core.EBusConsts;
import de.csdev.ebus.core.EBusController;
import de.csdev.ebus.core.connection.EBusEmulatorConnection;
import de.csdev.ebus.core.connection.EBusSerialNRJavaSerialConnection;
import de.csdev.ebus.core.connection.EBusTCPConnection;
import de.csdev.ebus.core.connection.IEBusConnection;
import de.csdev.ebus.utils.EBusUtils;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusLibClient {

    private final Logger logger = LoggerFactory.getLogger(EBusLibClient.class);

    private EBusController controller;

    private EBusClient client;

    private IEBusConnection connection;

    private ScheduledFuture<?> mockupSynJob;

    public EBusLibClient(EBusClientConfiguration configuration) {
        client = new EBusClient(configuration);
    }

    public void setTCPConnection(String hostname, int port) {
        connection = new EBusTCPConnection(hostname, port);
    }

    public void setMockupConnection(ScheduledExecutorService scheduler) {
        connection = new EBusEmulatorConnection(null);

        scheduler.schedule(new Runnable() {

            @Override
            public void run() {
                byte[] byteArray = EBusUtils.toByteArray("AA 03 FE 05 03 08 01 00 40 FF 2C 17 30 0E 96 AA");
                for (byte b : byteArray) {
                    try {
                        connection.writeByte(b);
                    } catch (IOException e) {
                        logger.error("error!", e);
                    }
                }

            }
        }, 10, TimeUnit.SECONDS);

        mockupSynJob = scheduler.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                try {
                    connection.writeByte(EBusConsts.SYN);
                } catch (IOException e) {
                    logger.error("error!", e);
                }
            }
        }, 20, 1, TimeUnit.SECONDS);
    }

    public void setSerialConnection(String serialPort) {
        connection = new EBusSerialNRJavaSerialConnection(serialPort);
    }

    public EBusClient getClient() {
        return client;
    }

    public boolean isConnectionValid() {
        return connection != null;
    }

    public ByteBuffer grrr(String commandId, IEBusCommandMethod.Method type, Thing targetThing)
            throws EBusTypeException {

        String slaveAddress = (String) targetThing.getConfiguration().get(EBusBindingConstants.SLAVE_ADDRESS);

        IEBusCommandMethod commandMethod = client.getConfigurationProvider().getConfigurationById(commandId, type);

        if (!commandMethod.getType().equals(IEBusCommandMethod.Type.MASTER_SLAVE)) {
            logger.warn("Polling is only available for master-slave commands!");
            return null;
        }

        if (StringUtils.isEmpty(slaveAddress)) {
            logger.warn("Unable to poll, Thing has no slave address defined!");
            return null;
        }

        byte target = EBusUtils.toByte(slaveAddress);

        return client.buildPollingTelegram(commandMethod, target);
    }

    public void initClient(Byte masterAddress) {

        // load the eBus core element
        controller = new EBusController(connection);

        // connect the high level client
        client.connect(controller, masterAddress);
    }

    public void startClient() {
        controller.start();
    }

    public void stopClient() {

        if (mockupSynJob != null) {
            mockupSynJob.cancel(true);
            mockupSynJob = null;
        }

        if (controller != null && !controller.isInterrupted()) {
            controller.interrupt();
        }
    }

}
