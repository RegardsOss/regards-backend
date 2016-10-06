/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.client.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
@Component
public class ClientErrorDecoder implements ErrorDecoder {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ClientErrorDecoder.class);

    @Override
    public Exception decode(String pMethodKey, Response pResponse) {

        LOG.error(String.format("Remote call to %s. Response is : %d - %s", pMethodKey, pResponse.status(), pResponse
                .reason()));
        return new Exception(String.format("%s:%d", pResponse.status(), pResponse.reason()));
    }
}
