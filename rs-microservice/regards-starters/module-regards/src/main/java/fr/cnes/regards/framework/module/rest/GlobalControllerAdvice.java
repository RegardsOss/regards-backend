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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityCorruptByNetworkException;
import fr.cnes.regards.framework.module.rest.exception.EntityDescriptionTooLargeException;
import fr.cnes.regards.framework.module.rest.exception.EntityDescriptionUnacceptableCharsetException;
import fr.cnes.regards.framework.module.rest.exception.EntityDescriptionUnacceptableType;
import fr.cnes.regards.framework.module.rest.exception.EntityEmbeddedEntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotEmptyException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotIdentifiableException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.InvalidConnectionException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;

/**
 * Global controller advice
 *
 * @author CS SI
 * @author Marc Sordi
 * @author Sébastien Binda
 * @since 1.1-SNAPSHOT
 *
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.LOWEST_PRECEDENCE - 1)
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

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ServerErrorResponse> handleModelException(final EntityAlreadyExistsException pEx) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ServerErrorResponse(pEx.getMessage()));
    }

    @ExceptionHandler(EntityEmbeddedEntityNotFoundException.class)
    public ResponseEntity<ServerErrorResponse> entityEmbeddedEntityNotFound(
            final EntityEmbeddedEntityNotFoundException pException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ServerErrorResponse(pException.getMessage()));
    }

    @ExceptionHandler(EntityDescriptionUnacceptableCharsetException.class)
    public ResponseEntity<ServerErrorResponse> entityDescriptionUnaccesptableCharset(
            final EntityDescriptionUnacceptableCharsetException pException) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ServerErrorResponse(pException.getMessage()));
    }

    @ExceptionHandler(EntityDescriptionUnacceptableType.class)
    public ResponseEntity<ServerErrorResponse> entityDescriptionUnaccesptableType(
            final EntityDescriptionUnacceptableType pException) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ServerErrorResponse(pException.getMessage()));
    }

    @ExceptionHandler(EntityDescriptionTooLargeException.class)
    public ResponseEntity<ServerErrorResponse> entityDescriptionTooLargeCharset(
            final EntityDescriptionTooLargeException pException) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ServerErrorResponse(pException.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ServerErrorResponse> entityNotFound(final EntityNotFoundException pException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ServerErrorResponse(pException.getMessage()));
    }

    @ExceptionHandler(EntityNotEmptyException.class)
    public ResponseEntity<ServerErrorResponse> entityNotEmpty(final EntityNotEmptyException pException) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ServerErrorResponse(pException.getMessage()));
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
     * Exception handler returning the code 422 when an entity in request violates its validation constraints.
     */
    @ExceptionHandler(EntityInvalidException.class)
    public ResponseEntity<ServerErrorResponse> manualValidation(final EntityInvalidException pException) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ServerErrorResponse(pException.getMessages()));
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

    /**
     *
     * Exception handler returning the code 403 when an operation on an entity is forbidden.<br>
     *
     * @param pException
     *            {@link EntityOperationForbiddenException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(EntityOperationForbiddenException.class)
    public ResponseEntity<ServerErrorResponse> entityOperationForbidden(
            final EntityOperationForbiddenException pException) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ServerErrorResponse(pException.getMessage()));
    }

    /**
     * Exception handler returning the code 403 when a transition on a state-managed entity is forbidden.<br>
     *
     * @param pException
     *            {@link EntityTransitionForbiddenException}
     * @return {@link ResponseEntity}
     *
     */
    @ExceptionHandler(EntityTransitionForbiddenException.class)
    public ResponseEntity<ServerErrorResponse> entityTransitionForbidden(
            final EntityTransitionForbiddenException pException) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ServerErrorResponse(pException.getMessage()));
    }

    /**
     * Exception handler returning the code 400 when the identifier in url path doesn't match identifier in request
     * body.<br>
     *
     * @param pException
     *            {@link EntityInconsistentIdentifierException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(EntityInconsistentIdentifierException.class)
    public ResponseEntity<ServerErrorResponse> entityInconsistentIdentifier(
            final EntityInconsistentIdentifierException pException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponse(pException.getMessage()));
    }

    /**
     * Spring framework Access denied exception. Throw by security methodAccessVoter
     *
     *
     * @param pException
     *            {@link AccessDeniedException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ServerErrorResponse> accessDeniedException(final AccessDeniedException pException) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ServerErrorResponse(pException.getMessage()));
    }

    @ExceptionHandler(EntityCorruptByNetworkException.class)
    public ResponseEntity<ServerErrorResponse> entityCorruptByNetworkException(
            final EntityCorruptByNetworkException pException) {
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                .body(new ServerErrorResponse(pException.getMessage()));
    }

    /**
     * Exception handler returning the code 4000 when an error occurs while processing an OpenSearch request.<br>
     *
     * @param pException
     *            {@link SearchException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(SearchException.class)
    public ResponseEntity<ServerErrorResponse> searchException(final SearchException pException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponse(
                pException.getMessage() + ". Cause: " + pException.getCause().getMessage()));
    }

    /**
     * Exception returning 400 when a datasource connection is invalid
     * 
     * @param pException
     *            {@link InvalidConnectionException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(InvalidConnectionException.class)
    public ResponseEntity<ServerErrorResponse> connectionException(final InvalidConnectionException pException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponse(
                pException.getMessage() + ". Cause: " + pException.getCause().getMessage()));
    }

}
