/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import feign.Response;
import feign.codec.ErrorDecoder;

/**
 *
 * Class ClientErrorDecoder
 *
 * Interceptor for Feign client errors.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class ClientErrorDecoder implements ErrorDecoder {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientErrorDecoder.class);

    /**
     *
     * Override method
     *
     * @see feign.codec.ErrorDecoder#decode(java.lang.String, feign.Response)
     * @since 1.0-SNAPSHOT
     */
    @Override
    public Exception decode(final String pMethodKey, final Response pResponse) {

        LOGGER.error(String.format("Remote call to %s. Response is : %d - %s", pMethodKey, pResponse.status(),
                                pResponse.reason()));
        return new Exception(String.format("%s:%s", pResponse.status(), pResponse.reason()));
    }
}
