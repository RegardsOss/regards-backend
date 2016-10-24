/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.rest;

import java.util.NoSuchElementException;

import javax.validation.ValidationException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidEntityException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 * Base controller class
 *
 * @author CS SI
 * @since 1.1-SNAPSHOT
 */
public class Controller {

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
    @ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY, reason = "Data does not respect validation constrains")
    public void manualValidation() {
        // Nothing to do. Just throw the exception.
    }

    /**
     * Exception handler returning the code 422 when an entity in request violates its validation constraints.<br>
     * Thrown by Hibernate.
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY, reason = "Data does not respect validation constrains")
    public void hibernateValidation() {
        // Nothing to do. Just throw the exception.
    }

}
