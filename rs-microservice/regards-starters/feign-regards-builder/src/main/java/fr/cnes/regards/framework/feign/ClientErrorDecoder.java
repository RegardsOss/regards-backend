/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * Intercept Feign error to write custom log and decode the body into an object
 * in case the return type is defined as a {@link ResponseEntity}&lt;SOMETHING>.
 * It will deserialize the body, using {@link org.springframework.cloud.openfeign.support.SpringDecoder}, into a
 * SOMETHING instance accessible
 * into the {@link FeignResponseDecodedException} thrown.
 *
 * @author CS
 */
public class ClientErrorDecoder extends ErrorDecoder.Default implements ErrorDecoder {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientErrorDecoder.class);

    @Override
    public Exception decode(final String methodKey, final Response response) {
        LOGGER.error(String.format("Remote call to %s. Response is : %d - %s",
                                   methodKey,
                                   response.status(),
                                   response.reason()));
        HttpHeaders responseHeaders = new HttpHeaders();
        response.headers().forEach((key, value) -> responseHeaders.put(key, new ArrayList<>(value)));

        byte[] responseBody = null;
        if (response.body() != null) {
            try {
                responseBody = response.body().asInputStream().readAllBytes();
            } catch (IOException e) {
                LOGGER.debug("Failed to process response body.", e);
                return super.decode(methodKey, response);
            }
        }

        Charset responseCharset = null;
        MediaType mediaType = responseHeaders.getContentType();
        if (mediaType != null) {
            // if we find any charset, lets use it
            responseCharset = mediaType.getCharset();
        }

        HttpStatus statusCode = HttpStatus.valueOf(response.status());
        String statusText = response.reason();
        if (response.status() >= 400 && response.status() <= 499) {
            return new HttpClientErrorException(statusCode, statusText, responseHeaders, responseBody, responseCharset);
        }

        if (response.status() >= 500 && response.status() <= 599) {
            return new HttpServerErrorException(statusCode, statusText, responseHeaders, responseBody, responseCharset);
        }
        return super.decode(methodKey, response);
    }
}
