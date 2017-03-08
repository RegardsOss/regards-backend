/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * Intercept Feign error to write custom log and propagate decoding to default decoder.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class ClientErrorDecoder extends ErrorDecoder.Default implements ErrorDecoder {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientErrorDecoder.class);

    @Override
    public Exception decode(final String pMethodKey, final Response pResponse) {

        LOGGER.error(String.format("Remote call to %s. Response is : %d - %s", pMethodKey, pResponse.status(),
                                   pResponse.reason()));
        return super.decode(pMethodKey, pResponse);
    }
}
