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
package fr.cnes.regards.framework.module.rest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Advice for back-for-frontend exceptions
 *
 * @author Sylvain Vissiere-Guerinet
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(0)
public class FeignControllerAdvice {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeignControllerAdvice.class);

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<JsonObject> httpClientErrorException(final HttpStatusCodeException exception) {
        return buildError(exception);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<JsonObject> httpServerErrorException(final HttpServerErrorException exception) {
        return buildError(exception);
    }

    private ResponseEntity<JsonObject> buildError(HttpStatusCodeException exception) {
        LOGGER.error(exception.getMessage(), exception);
        JsonObject jsonObject = JsonParser.parseString(exception.getResponseBodyAsString()).getAsJsonObject();
        return ResponseEntity.status(exception.getStatusCode()).body(jsonObject);
    }
}
