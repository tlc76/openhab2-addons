/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal;

import java.net.URI;
import java.net.URISyntaxException;

import org.openhab.binding.ebus.EBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
public class EbusBindingUtils {

    private static final Logger logger = LoggerFactory.getLogger(EBusBindingConstants.class);

    public static URI getURI(String id) {
        try {
            return new URI(id);
        } catch (URISyntaxException e) {
            logger.error("error!", e);
        }
        return null;
    }

}
