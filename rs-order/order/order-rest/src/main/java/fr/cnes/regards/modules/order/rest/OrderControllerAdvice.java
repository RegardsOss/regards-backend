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
package fr.cnes.regards.modules.order.rest;

import java.util.NoSuchElementException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;
import fr.cnes.regards.modules.order.domain.exception.BadBasketSelectionRequestException;
import fr.cnes.regards.modules.order.domain.exception.CannotDeleteOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotPauseOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotRemoveOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotResumeOrderException;
import fr.cnes.regards.modules.order.domain.exception.EmptyBasketException;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;
import fr.cnes.regards.modules.order.domain.exception.NotYetAvailableException;

/**
 * @author oroussel
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(0)
public class OrderControllerAdvice {

    @ExceptionHandler(EmptyBasketException.class)
    public ResponseEntity<ServerErrorResponse> handleEmptyBasketException(EmptyBasketException ebe) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new ServerErrorResponse(ebe.getMessage()));
    }

    @ExceptionHandler(EmptySelectionException.class)
    public ResponseEntity<ServerErrorResponse> handleEmptySelectionException(EmptySelectionException ebe) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new ServerErrorResponse(ebe.getMessage()));
    }

    @ExceptionHandler(NotYetAvailableException.class)
    public ResponseEntity<ServerErrorResponse> handleNotYetAvailableException(NotYetAvailableException e) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ServerErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(CannotPauseOrderException.class)
    public ResponseEntity<ServerErrorResponse> handleCannotPauseOrderException(CannotPauseOrderException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ServerErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(CannotDeleteOrderException.class)
    public ResponseEntity<ServerErrorResponse> handleCannotDeleteOrderException(CannotDeleteOrderException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ServerErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(CannotResumeOrderException.class)
    public ResponseEntity<ServerErrorResponse> handleCannotResumeOrderException(CannotResumeOrderException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ServerErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(CannotRemoveOrderException.class)
    public ResponseEntity<ServerErrorResponse> handleCannotRemoveOrderException(CannotRemoveOrderException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ServerErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(BadBasketSelectionRequestException.class)
    public ResponseEntity<ServerErrorResponse> handleBadBasketSelectionRequestException(
            BadBasketSelectionRequestException e) {
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ServerErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ServerErrorResponse> handleNoSuchElementException(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ServerErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ServerErrorResponse> handleIllegalStateException(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ServerErrorResponse(e.getMessage()));
    }
}