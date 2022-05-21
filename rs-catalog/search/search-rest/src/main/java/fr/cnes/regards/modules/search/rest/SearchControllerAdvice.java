/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest;

import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;
import fr.cnes.regards.modules.search.service.SearchException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Advice for specific search exceptions
 *
 * @author Marc Sordi
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(0)
public class SearchControllerAdvice {

    /**
     * Exception handler returning the code 400 when an error occurs while processing an OpenSearch request.<br>
     *
     * @param exception {@link SearchException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(SearchException.class)
    public ResponseEntity<ServerErrorResponse> searchException(final SearchException exception) {
        String message = exception.getMessage();
        if (exception.getCause() != null) {
            message += ". Cause: " + exception.getCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponse(message, exception));
    }

    @ExceptionHandler(OpenSearchParseException.class)
    public ResponseEntity<ServerErrorResponse> openSearchParseException(final OpenSearchParseException exception) {
        String message = exception.getMessage();
        if (exception.getCause() != null) {
            message += ". Cause: " + exception.getCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponse(message, exception));
    }
}
