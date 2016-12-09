/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods
 *
 * @author Marc Sordi
 *
 */
public final class GSONUtils {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GSONUtils.class);

    private GSONUtils() {
    }

    public static void assertNotNull(Object pObject, String pErrorMessage) {
        if (pObject == null) {
            LOGGER.error(pErrorMessage);
            throw new IllegalArgumentException(pErrorMessage);
        }
    }

    public static void assertNotNullOrEmpty(String pObject, String pErrorMessage) {
        assertNotNull(pObject, pErrorMessage);
        if (pObject.isEmpty()) {
            LOGGER.error(pErrorMessage);
            throw new IllegalArgumentException(pErrorMessage);
        }
    }
}
