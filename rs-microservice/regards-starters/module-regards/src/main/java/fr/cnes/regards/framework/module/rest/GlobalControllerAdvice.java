/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest;

import java.util.NoSuchElementException;

import javax.validation.ValidationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.AlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.InvalidEntityException;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.rest.exception.OperationForbiddenException;
import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;

/**
 * Global controller advice
 *
 * @author CS SI
 * @author Marc Sordi
 * @since 1.1-SNAPSHOT
 */
@RestControllerAdvice(annotations = RestController.class)
public class GlobalControllerAdvice {

    @ExceptionHandler(ModuleException.class)
    public ResponseEntity<ServerErrorResponse> handleModelException(final ModuleException pEx) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ServerErrorResponse("Internal server error"));
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ServerErrorResponse> handleModelException(final AlreadyExistsException pEx) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ServerErrorResponse(pEx.getMessage()));
    }

    /**
     * Exception handler returning the code 404 when a requested entity does not exists.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Data Not Found")
    public void dataNotFound() {
        // Nothing to do. Just throw the exception.
    }

    /**
     * Exception handler returning the code 404 when an element accessed does not exists (for example in a stream).
     */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Data Not Found")
    public void noSuchElement() {
        // Nothing to do. Just throw the exception.
    }

    /**
     * Exception handler returning the code 409 when trying to create an already existing entity.
     */
    @ExceptionHandler(AlreadyExistingException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public void dataAlreadyExisting() {
        // Nothing to do. Just throw the exception.
    }

    /**
     * Exception handler returning the code 400 when the request is somehow malformed or invalid.
     */
    @ExceptionHandler(InvalidValueException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public void invalidValue() {
        // Nothing to do. Just throw the exception.
    }

    /**
     * Exception handler returning the code 422 when an entity in request violates its validation constraints.
     */
    @ExceptionHandler(InvalidEntityException.class)
    @ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY, reason = "Data does not respect validation constraints")
    public void manualValidation() {
        // Nothing to do. Just throw the exception.
    }

    /**
     * Exception handler returning the code 422 when an entity in request violates its validation constraints.<br>
     * Thrown by Hibernate.
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY, reason = "Data does not respect validation constraints")
    public void hibernateValidation() {
        // Nothing to do. Just throw the exception.
    }

    /**
     * Exception handler returning the code 403 when an operation on an entity is forbidden.<br>
     * Thrown by Hibernate.
     */
    @ExceptionHandler(OperationForbiddenException.class)
    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    public void operationForbidden() {
        // Nothing to do. Just throw the exception.
    }
}
