/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal;

import static org.openhab.binding.ebus.EBusBindingConstants.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.ebus.EBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.client.EBusClientConfiguration;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.IEBusCommandMethod.Method;
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

    private void sendRawData(long sleepBefore, String data) {
        byte[] byteArray = EBusUtils.toByteArray(data);
        ((EBusEmulatorConnection) connection).writeBytesDelayed(byteArray, sleepBefore);
    }

    public void setDummyConnection(ScheduledExecutorService scheduler) {
        connection = new EBusEmulatorConnection(null);

        scheduler.schedule(new Runnable() {

            @Override
            public void run() {

                // wolf solar e1
                EBusLibClient.this.sendRawData(0, "AA 03 FE 05 03 08 01 00 40 FF 2C 17 30 0E 96 AA");

                // wolf solar a
                EBusLibClient.this.sendRawData(5000, "AA 71 FE 50 18 0E 00 00 D0 01 05 00 E2 03 0F 01 01 00 00 00 18");

                // wolf solar b
                EBusLibClient.this.sendRawData(10000,
                        "AA 71 FE 50 17 10 08 91 05 01 CA 01 00 80 00 80 00 80 00 80 00 80 9B");

                // auto stroker
                EBusLibClient.this.sendRawData(15000, "03 FE 05 03 08 01 00 40 FF 2C 17 30 0E 96 AA");

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

    public Integer sendTelegram(ByteBuffer telegram) {
        return client.getController().addToSendQueue(telegram);
    }

    public ByteBuffer generateSetterTelegram(Thing thing, Channel channel, Command command) throws EBusTypeException {

        String slaveAddress = (String) thing.getConfiguration().get(EBusBindingConstants.SLAVE_ADDRESS);
        String commandId = channel.getProperties().get(COMMAND);
        String valueName = channel.getProperties().get(VALUE_NAME);

        if (StringUtils.isEmpty(commandId) || StringUtils.isEmpty(valueName)) {
            logger.error("Channel has no additional eBUS information!");
            return null;
        }

        IEBusCommandMethod commandMethod = client.getConfigurationProvider().getConfigurationById(commandId,
                Method.SET);

        if (commandMethod == null) {
            logger.error("Unable to find setter command with id {}", commandId);
            return null;
        }

        HashMap<String, Object> values = new HashMap<>();

        if (command instanceof State) {
            State state = (State) command;
            DecimalType decimalValue = (DecimalType) state.as(DecimalType.class);

            if (decimalValue != null) {
                values.put(valueName, decimalValue.toBigDecimal());
            }
        }

        byte target = EBusUtils.toByte(slaveAddress);
        return client.buildTelegram(commandMethod, target, values);
    }

    public ByteBuffer generatePollingTelegram(String commandId, IEBusCommandMethod.Method type, Thing targetThing)
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

        return client.buildTelegram(commandMethod, target, null);
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
