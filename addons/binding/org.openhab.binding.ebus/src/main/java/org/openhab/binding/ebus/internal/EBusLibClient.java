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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
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

    // private ScheduledFuture<?> mockupSynJob;

    /**
     * @param configuration
     */
    public EBusLibClient(EBusClientConfiguration configuration) {
        client = new EBusClient(configuration);
    }

    /**
     * @param hostname
     * @param port
     */
    public void setTCPConnection(String hostname, int port) {
        connection = new EBusTCPConnection(hostname, port);
    }

    /**
     * @param sleepBefore
     * @param data
     */
    private void sendRawData(long sleepBefore, String data) {
        byte[] byteArray = EBusUtils.toByteArray(data);
        ((EBusEmulatorConnection) connection).writeBytesDelayed(byteArray, sleepBefore);
    }

    /**
     * @param scheduler
     */
    public void setDummyConnection(ScheduledExecutorService scheduler) {
        connection = new EBusEmulatorConnection();

        scheduler.schedule(new Runnable() {

            @Override
            public void run() {

                EBusLibClient.this.sendRawData(200, "30 03 05 07 09 00 03 50 00 00 80 FF 14 FF CF 00 AA");

                EBusLibClient.this.sendRawData(200, "FF 35 07 04 00 E4 00 0A 19 00 20 00 00 C0 02 04 00 00 DB 00");

                // wolf solar e1
                EBusLibClient.this.sendRawData(0, "AA 03 FE 05 03 08 01 00 40 FF 2C 17 30 0E 96 AA");

                // wolf solar a
                EBusLibClient.this.sendRawData(500, "AA 71 FE 50 18 0E 00 00 D0 01 05 00 E2 03 0F 01 01 00 00 00 18");

                // wolf solar b
                EBusLibClient.this.sendRawData(100,
                        "AA 71 FE 50 17 10 08 91 05 01 CA 01 00 80 00 80 00 80 00 80 00 80 9B");

                // auto stroker
                EBusLibClient.this.sendRawData(1500, "03 FE 05 03 08 01 00 40 FF 2C 17 30 0E 96 AA");

                // number with option
                logger.info("RUN ..............");
                EBusLibClient.this.sendRawData(100, "AA FF 08 50 22 03 11 74 27 2C 00 02 00 80 AC 00 AA");

                EBusLibClient.this.sendRawData(100, "AA FF 08 B5 09 03 0D 28 00 EA 00 02 FE 0E C2 00 AA");

            }
        }, 5, TimeUnit.SECONDS);

        // mockupSynJob = scheduler.scheduleWithFixedDelay(new Runnable() {
        //
        // @Override
        // public void run() {
        // try {
        // connection.writeByte(EBusConsts.SYN);
        // } catch (IOException e) {
        // logger.error("error!", e);
        // }
        // }
        // }, 10, 1, TimeUnit.SECONDS);
    }

    /**
     * @param serialPort
     */
    public void setSerialConnection(String serialPort) {
        connection = new EBusSerialNRJavaSerialConnection(serialPort);
    }

    /**
     * @return
     */
    public EBusClient getClient() {
        return client;
    }

    /**
     * @return
     */
    public boolean isConnectionValid() {
        return connection != null;
    }

    /**
     * @param telegram
     * @return
     */
    public Integer sendTelegram(ByteBuffer telegram) {
        return client.addToSendQueue(EBusUtils.toByteArray(telegram));
    }

    /**
     * @param telegram
     * @return
     */
    public Integer sendTelegram(byte[] telegram) {
        return client.addToSendQueue(telegram);
    }

    /**
     * @param thing
     * @param channel
     * @param command
     * @return
     * @throws EBusTypeException
     */
    public ByteBuffer generateSetterTelegram(Thing thing, Channel channel, Command command) throws EBusTypeException {

        String slaveAddress = (String) thing.getConfiguration().get(EBusBindingConstants.SLAVE_ADDRESS);
        String collectionId = channel.getProperties().get(COLLECTION);
        String commandId = channel.getProperties().get(COMMAND);
        String valueName = channel.getProperties().get(VALUE_NAME);

        if (StringUtils.isEmpty(commandId) || StringUtils.isEmpty(valueName)) {
            logger.error("Channel has no additional eBUS information!");
            return null;
        }

        IEBusCommandMethod commandMethod = client.getConfigurationProvider().getCommandMethodById(collectionId,
                commandId, Method.SET);

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

    /**
     * @param collectionId
     * @param commandId
     * @param type
     * @param targetThing
     * @return
     * @throws EBusTypeException
     */
    public ByteBuffer generatePollingTelegram(String collectionId, String commandId, IEBusCommandMethod.Method type,
            Thing targetThing) throws EBusTypeException {

        String slaveAddress = (String) targetThing.getConfiguration().get(EBusBindingConstants.SLAVE_ADDRESS);

        IEBusCommandMethod commandMethod = client.getConfigurationProvider().getCommandMethodById(collectionId,
                commandId, type);

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

    /**
     * @param masterAddress
     */
    public void initClient(Byte masterAddress) {

        // load the eBus core element
        controller = new EBusController(connection);

        // connect the high level client
        client.connect(controller, masterAddress);
    }

    /**
     *
     */
    public void startClient() {
        controller.start();
    }

    /**
     *
     */
    public void stopClient() {
        //
        // if (mockupSynJob != null) {
        // mockupSynJob.cancel(true);
        // mockupSynJob = null;
        // }

        if (controller != null && !controller.isInterrupted()) {
            controller.interrupt();
        }
    }

}
