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

    private static final Logger LOG = LoggerFactory.getLogger(ClientErrorDecoder.class);

    @Override
    public Exception decode(String methodKey, Response response) {

        LOG.error("Remote call to " + methodKey + ". Response is " + response.status() + " : " + response.reason());
        return new Exception(response.status() + " : " + response.reason());
    }
}
