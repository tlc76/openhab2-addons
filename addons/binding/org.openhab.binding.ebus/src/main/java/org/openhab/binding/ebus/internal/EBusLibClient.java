/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
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
import de.csdev.ebus.command.EBusCommandRegistry;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.IEBusCommandMethod.Method;
import de.csdev.ebus.command.datatypes.EBusTypeException;
import de.csdev.ebus.core.EBusController;
import de.csdev.ebus.core.EBusControllerException;
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

    /**
     * @param configuration
     */
    public EBusLibClient(EBusCommandRegistry commandRegistry) {
        client = new EBusClient(commandRegistry);
    }

    /**
     * @param hostname
     * @param port
     */
    public void setTCPConnection(String hostname, int port) {
        connection = new EBusTCPConnection(hostname, port);
    }

    /**
     * @param serialPort
     */
    public void setSerialConnection(String serialPort) {
        if (serialPort.equals("emulator")) {
            connection = new EBusEmulatorConnection();

        } else {
            connection = new EBusSerialNRJavaSerialConnection(serialPort);
        }
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
     * @throws EBusControllerException
     */
    public Integer sendTelegram(ByteBuffer telegram) throws EBusControllerException {
        return client.addToSendQueue(EBusUtils.toByteArray(telegram));
    }

    /**
     * @param telegram
     * @return
     * @throws EBusControllerException
     */
    public Integer sendTelegram(byte[] telegram) throws EBusControllerException {
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
        String collectionId = thing.getThingTypeUID().getId();
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
            DecimalType decimalValue = state.as(DecimalType.class);

            if (decimalValue == null) {
                decimalValue = new DecimalType(state.toString());
            }

            values.put(valueName, decimalValue.toBigDecimal());
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

        if (commandMethod == null) {
            logger.error("Unable to find command method {} {} {} !", type, commandId, collectionId);
            return null;
        }

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

        if (controller != null && !controller.isInterrupted()) {
            controller.interrupt();
        }
    }

}
