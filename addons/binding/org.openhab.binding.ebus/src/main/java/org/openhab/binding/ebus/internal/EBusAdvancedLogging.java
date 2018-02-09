/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.utils.EBusTelegramWriter;

/**
 * An openhab variant of the ebus core logger
 * 
 * @author Christian Sowada - Initial contribution
 */
public class EBusAdvancedLogging extends EBusTelegramWriter {

    @SuppressWarnings("unused")
    private final Logger logger = LoggerFactory.getLogger(EBusAdvancedLogging.class);

    public EBusAdvancedLogging() {
        super(new File(System.getProperty("openhab.logdir")));
    }

}
