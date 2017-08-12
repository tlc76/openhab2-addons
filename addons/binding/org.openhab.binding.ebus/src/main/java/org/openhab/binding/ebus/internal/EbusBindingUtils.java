package org.openhab.binding.ebus.internal;

import java.net.URI;
import java.net.URISyntaxException;

import org.openhab.binding.ebus.EBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
