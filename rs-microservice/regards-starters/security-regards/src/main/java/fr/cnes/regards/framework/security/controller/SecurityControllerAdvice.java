/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;

/**
 * Controller advice
 *
 * @author Marc Sordi
 *
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.LOWEST_PRECEDENCE - 200)
public class SecurityControllerAdvice {

    /**
     * Spring framework Access denied exception. Throw by security methodAccessVoter
     *
     * @param pException {@link AccessDeniedException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ServerErrorResponse> accessDeniedException(final AccessDeniedException pException) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ServerErrorResponse(pException.getMessage()));
    }
}
