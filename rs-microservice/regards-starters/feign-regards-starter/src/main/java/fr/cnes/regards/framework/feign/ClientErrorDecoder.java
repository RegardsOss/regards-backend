/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
