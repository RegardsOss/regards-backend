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
package fr.cnes.regards.modules.indexer.rest;

import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;
import fr.cnes.regards.modules.indexer.dao.exception.ESIndexNotFoundRuntimeException;
import fr.cnes.regards.modules.indexer.dao.exception.FieldNotIndexedRuntimeException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Advice for specific indexer exceptions
 *
 * @author Thibaud Michaudel
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(10)
public class IndexerControllerAdvice {

    @ExceptionHandler(ESIndexNotFoundRuntimeException.class)
    public ResponseEntity<ServerErrorResponse> esIndexNotfoundException(final ESIndexNotFoundRuntimeException exception) {
        String message = exception.getMessage();
        if (exception.getCause() != null) {
            message += ". Cause: " + exception.getCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ServerErrorResponse(message, exception));
    }

    @ExceptionHandler(FieldNotIndexedRuntimeException.class)
    public ResponseEntity<ServerErrorResponse> fieldNotIndexedException(final FieldNotIndexedRuntimeException exception) {
        String message = exception.getMessage();
        if (exception.getCause() != null) {
            message += ". Cause: " + exception.getCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ServerErrorResponse(message, exception));
    }

}
