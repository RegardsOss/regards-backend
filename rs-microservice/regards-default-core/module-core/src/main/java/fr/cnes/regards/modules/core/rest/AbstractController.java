/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.rest;

import java.util.NoSuchElementException;

import javax.validation.ValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.microservices.core.manage.MaintenanceManager;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidEntityException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 * Base controller class
 *
 * @author CS SI
 * @author Sylvain Vissiere-Guerinet
 * @since 1.1-SNAPSHOT
 */
public abstract class AbstractController {

    @Autowired
    private JWTService jwtService;

    /**
     * Exception handler returning the code 404 when a requested entity does not exists.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> dataNotFound(EntityNotFoundException pException) {
        return new ResponseEntity<>(pException.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Exception handler returning the code 404 when an element accessed does not exists (for example in a stream).
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> noSuchElement(NoSuchElementException pException) {
        return new ResponseEntity<>(pException.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Exception handler returning the code 409 when trying to create an already existing entity.
     */
    @ExceptionHandler(AlreadyExistingException.class)
    public ResponseEntity<String> dataAlreadyExisting(AlreadyExistingException pException) {
        return new ResponseEntity<>(pException.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Exception handler returning the code 400 when the request is somehow malformed or invalid.
     */
    @ExceptionHandler(InvalidValueException.class)
    public ResponseEntity<String> invalidValue(InvalidValueException pException) {
        return new ResponseEntity<>(pException.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Exception handler returning the code 422 when an entity in request violates its validation constraints.
     */
    @ExceptionHandler(InvalidEntityException.class)
    public ResponseEntity<String> manualValidation(InvalidEntityException pException) {
        return new ResponseEntity<>(pException.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Exception handler returning the code 422 when an entity in request violates its validation constraints.<br>
     * Thrown by Hibernate.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<String> hibernateValidation(ValidationException pException) {
        return new ResponseEntity<>(pException.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     *
     * @param pException
     *            exception thrown
     * @return response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> nonFunctionnalException(Exception pException) {
        String tenant = jwtService.getActualTenant();
        MaintenanceManager.setMaintenance(tenant);
        return new ResponseEntity<>(pException.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
    }
}
