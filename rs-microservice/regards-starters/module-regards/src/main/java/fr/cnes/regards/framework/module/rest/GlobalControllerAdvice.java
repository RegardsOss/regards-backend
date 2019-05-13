/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotEmptyException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.InvalidConnectionException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.rest.exception.TooManyResultsException;
import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;

/**
 * Global controller advice manages generic system exceptions handling<br/>
 *
 * Following generic exception can be used to manage business work flow. If no exception fits your need, create your own
 * extending {@link ModuleException} and add a {@link RestControllerAdvice} in your rest layer handling this specific
 * exception. Be careful to set an {@link Order} with highest precedence.<br/>
 *
 * <br/>
 * To handle {@link HttpStatus#BAD_REQUEST} (400), you may throw :
 * <ul>
 * <li>{@link EntityInconsistentIdentifierException}</li>
 * <li>{@link InvalidConnectionException}</li>
 * </ul>
 * <br/>
 * To handle {@link HttpStatus#FORBIDDEN} (403), you may throw :
 * <ul>
 * <li>{@link EntityOperationForbiddenException}</li>
 * <li>{@link EntityTransitionForbiddenException}</li>
 * </ul>
 * <br/>
 * To handle {@link HttpStatus#NOT_FOUND} (404), you may throw :
 * <ul>
 * <li>{@link EntityNotFoundException}</li>
 * </ul>
 * <br/>
 * To handle {@link HttpStatus#CONFLICT} (409), you may throw :
 * <ul>
 * <li>{@link EntityAlreadyExistsException}</li>
 * <li>{@link EntityNotEmptyException}</li>
 * </ul>
 * <br/>
 * To handle {@link HttpStatus#PAYLOAD_TOO_LARGE} (413), you may throw :
 * <ul>
 * <li>{@link TooManyResultsException}</li>
 * </ul>
 * <br/>
 * To handle {@link HttpStatus#UNPROCESSABLE_ENTITY} (422), you may throw :
 * <ul>
 * <li>{@link EntityInvalidException}</li>
 * </ul>
 * Bean validation exceptions also generate {@link HttpStatus#UNPROCESSABLE_ENTITY} :
 * <ul>
 * <li>{@link ValidationException}</li>
 * <li>{@link MethodArgumentNotValidException}</li>
 * </ul>
 * <br/>
 * if no handler is specified for a {@link ModuleException}, an {@link HttpStatus#INTERNAL_SERVER_ERROR} response is
 * sent.
 * @author CS SI
 * @author Marc Sordi
 * @author Sébastien Binda
 * @since 1.1-SNAPSHOT
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.LOWEST_PRECEDENCE - 300)
public class GlobalControllerAdvice extends ResponseEntityExceptionHandler {

    /**
     * Formatting constant
     */
    private static final String CAUSE = ". Cause :";

    /**
     * Customize response body for spring managed exception
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatus status, WebRequest request) {

        // Create REGARDS body
        ServerErrorResponse responseBody = new ServerErrorResponse(ex.getMessage(), ex);

        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        }
        return new ResponseEntity<>(responseBody, headers, status);
    }

    // ***************************************************************************************************************
    // HTTP 500
    // ***************************************************************************************************************

    /**
     * Default {@link ModuleException} response fallback
     * @param moduleException {@link ModuleException}
     * @return response with {@link HttpStatus#INTERNAL_SERVER_ERROR}
     */
    @ExceptionHandler(ModuleException.class)
    public ResponseEntity<ServerErrorResponse> handleModelException(final ModuleException moduleException) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ServerErrorResponse(moduleException.getMessage(), moduleException));
    }

    // ***************************************************************************************************************
    // HTTP 400
    // ***************************************************************************************************************

    /**
     * Exception handler returning the code 400 when the identifier in url path doesn't match identifier in request
     * body.
     * @param exception {@link EntityInconsistentIdentifierException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(EntityInconsistentIdentifierException.class)
    public ResponseEntity<ServerErrorResponse> entityInconsistentIdentifier(
            final EntityInconsistentIdentifierException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ServerErrorResponse(exception.getMessage(), exception));
    }

    /**
     * Exception returning 400 when a datasource connection is invalid
     * @param exception {@link InvalidConnectionException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(InvalidConnectionException.class)
    public ResponseEntity<ServerErrorResponse> connectionException(final InvalidConnectionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ServerErrorResponse(exception.getMessage() + CAUSE + exception.getCause().getMessage(),
                                              exception));
    }

    // ***************************************************************************************************************
    // HTTP 403
    // ***************************************************************************************************************

    /**
     * Exception handler returning the code 403 when an operation on an entity is forbidden.<br>
     * @param exception {@link EntityOperationForbiddenException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(EntityOperationForbiddenException.class)
    public ResponseEntity<ServerErrorResponse> entityOperationForbidden(
            final EntityOperationForbiddenException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ServerErrorResponse(exception.getMessage(), exception));
    }

    /**
     * Exception handler returning the code 403 when a transition on a state-managed entity is forbidden.<br>
     * @param exception {@link EntityTransitionForbiddenException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(EntityTransitionForbiddenException.class)
    public ResponseEntity<ServerErrorResponse> entityTransitionForbidden(
            final EntityTransitionForbiddenException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ServerErrorResponse(exception.getMessage(), exception));
    }

    // ***************************************************************************************************************
    // HTTP 404
    // ***************************************************************************************************************

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ServerErrorResponse> entityNotFound(final EntityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ServerErrorResponse(exception.getMessage(), exception));
    }

    // ***************************************************************************************************************
    // HTTP 409
    // ***************************************************************************************************************

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ServerErrorResponse> handleEntityAlreadyExistsException(
            final EntityAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ServerErrorResponse(exception.getMessage(), exception));
    }

    /**
     * Exception handler for {@link EntityNotEmptyException}
     */
    @ExceptionHandler(EntityNotEmptyException.class)
    public ResponseEntity<ServerErrorResponse> entityNotEmpty(final EntityNotEmptyException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ServerErrorResponse(exception.getMessage(), exception));
    }

    // ***************************************************************************************************************
    // HTTP 413
    // ***************************************************************************************************************

    /**
     * Exception handler returning the code 413 when a search is cancelled due to too many results.
     * @param pException {@link TooManyResultsException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(TooManyResultsException.class)
    public ResponseEntity<ServerErrorResponse> tooManyResultsException(final TooManyResultsException pException) {
        String message = pException.getMessage();
        if (pException.getCause() != null) {
            message += CAUSE + pException.getCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ServerErrorResponse(message, pException));
    }

    // ***************************************************************************************************************
    // HTTP 422
    // ***************************************************************************************************************

    /**
     * Exception handler returning the code 422 when an entity in request violates its validation constraints.
     * @param exception {@link EntityInvalidException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(EntityInvalidException.class)
    public ResponseEntity<ServerErrorResponse> manualValidation(final EntityInvalidException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ServerErrorResponse(exception.getMessages(), exception));
    }

    /**
     * Exception handler returning the code 422 when an entity in request violates its validation constraints.<br>
     * Thrown by Hibernate.
     * @param exception {@link ValidationException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ServerErrorResponse> hibernateValidation(final ValidationException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ServerErrorResponse(exception.getMessage(), exception));
    }

    /**
     * Specification of the handler for MethodArgumentNotValidException. In REGARDS, we send a 422
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {

        List<String> messages = new ArrayList<>();
        // Only return default messages at the moment
        ex.getBindingResult().getAllErrors().forEach(objectError -> messages.add(objectError.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ServerErrorResponse(messages, ex));
    }
}
