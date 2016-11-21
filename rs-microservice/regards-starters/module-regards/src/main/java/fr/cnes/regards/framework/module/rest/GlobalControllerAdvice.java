/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest;

import java.util.NoSuchElementException;

import javax.validation.ValidationException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotEmptyException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotIdentifiableException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.InvalidEntityException;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.ModuleAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotEmptyException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotIdentifiableException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.rest.exception.OperationForbiddenException;
import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;

/**
 * Global controller advice
 *
 * @author CS SI
 * @author Marc Sordi
 * @author Sébastien Binda
 * @since 1.1-SNAPSHOT
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ServerErrorResponse> handleArgumentValidationFailedException(final ModuleException pEx) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponse(pEx.getMessage()));
    }

    @ExceptionHandler(ModuleException.class)
    public ResponseEntity<ServerErrorResponse> handleModelException(final ModuleException pEx) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ServerErrorResponse("Internal server error"));
    }

    @ExceptionHandler(ModuleAlreadyExistsException.class)
    public ResponseEntity<ServerErrorResponse> handleModelException(final ModuleAlreadyExistsException pEx) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ServerErrorResponse(pEx.getMessage()));
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ServerErrorResponse> handleModelException(final EntityAlreadyExistsException pEx) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ServerErrorResponse(pEx.getMessage()));
    }

    @ExceptionHandler(ModuleEntityNotFoundException.class)
    public ResponseEntity<ServerErrorResponse> dataNotFound(final ModuleEntityNotFoundException pException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ServerErrorResponse(pException.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ServerErrorResponse> dataNotFound(final EntityNotFoundException pException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ServerErrorResponse(pException.getMessage()));
    }

    @ExceptionHandler(ModuleEntityNotEmptyException.class)
    public ResponseEntity<ServerErrorResponse> entityNotEmpty(final ModuleEntityNotEmptyException pException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ServerErrorResponse(pException.getMessage()));
    }

    @ExceptionHandler(EntityNotEmptyException.class)
    public ResponseEntity<ServerErrorResponse> entityNotEmpty(final EntityNotEmptyException pException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ServerErrorResponse(pException.getMessage()));
    }

    @ExceptionHandler(ModuleEntityNotIdentifiableException.class)
    public ResponseEntity<ServerErrorResponse> entityNotIdentifiable(
            final ModuleEntityNotIdentifiableException pException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ServerErrorResponse(pException.getMessage()));
    }

    @ExceptionHandler(EntityNotIdentifiableException.class)
    public ResponseEntity<ServerErrorResponse> entityNotIdentifiable(final EntityNotIdentifiableException pException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ServerErrorResponse(pException.getMessage()));
    }

    /*
     * Exception handler returning the code 404 when an element accessed does not exists (for example in a stream).
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ServerErrorResponse> noSuchElement(final NoSuchElementException pException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ServerErrorResponse(pException.getMessage()));
    }

    /*
     * Exception handler returning the code 409 when trying to create an already existing entity.
     */
    @ExceptionHandler(AlreadyExistingException.class)
    public ResponseEntity<ServerErrorResponse> dataAlreadyExisting(final AlreadyExistingException pException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ServerErrorResponse(pException.getMessage()));
    }

    /*
     * Exception handler returning the code 400 when the request is somehow malformed or invalid.
     */
    @ExceptionHandler(InvalidValueException.class)
    public ResponseEntity<ServerErrorResponse> invalidValue(final InvalidValueException pException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponse(pException.getMessage()));
    }

    /*
     * Exception handler returning the code 422 when an entity in request violates its validation constraints.
     */
    @ExceptionHandler(InvalidEntityException.class)
    public ResponseEntity<ServerErrorResponse> manualValidation(final InvalidEntityException pException) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ServerErrorResponse(pException.getMessage()));
    }

    /*
     * Exception handler returning the code 422 when an entity in request violates its validation constraints.
     */
    @ExceptionHandler(EntityInvalidException.class)
    public ResponseEntity<ServerErrorResponse> manualValidation(final EntityInvalidException pException) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ServerErrorResponse(pException.getMessage()));
    }

    /*
     * Exception handler returning the code 422 when an entity in request violates its validation constraints.<br>
     * Thrown by Hibernate.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ServerErrorResponse> hibernateValidation(final ValidationException pException) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ServerErrorResponse(pException.getMessage()));
    }

    /*
     * Exception handler returning the code 403 when an operation on an entity is forbidden.<br> Thrown by Hibernate.
     */
    @ExceptionHandler(OperationForbiddenException.class)
    public ResponseEntity<ServerErrorResponse> operationForbidden(final OperationForbiddenException pException) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ServerErrorResponse(pException.getMessage()));
    }

    /**
     * Exception handler returning the code 403 when an operation on an entity is forbidden.<br>
     */
    @ExceptionHandler(EntityOperationForbiddenException.class)
    public ResponseEntity<ServerErrorResponse> entityOperationForbidden(
            final EntityOperationForbiddenException pException) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ServerErrorResponse(pException.getMessage()));
    }

    /**
     * Exception handler returning the code 403 when a transition on a state-managed entity is forbidden.<br>
     */
    @ExceptionHandler(EntityTransitionForbiddenException.class)
    public ResponseEntity<ServerErrorResponse> entityTransitionForbidden(
            final EntityTransitionForbiddenException pException) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ServerErrorResponse(pException.getMessage()));
    }

    /**
     * Exception handler returning the code 400 when the identifier in url path doesn't match identifier in request
     * body.<br>
     */
    @ExceptionHandler(EntityInconsistentIdentifierException.class)
    public ResponseEntity<ServerErrorResponse> entityInconsistentIdentifier(
            final EntityInconsistentIdentifierException pException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponse(pException.getMessage()));
    }
}
