package fr.cnes.regards.modules.backendforfrontend.rest;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;

/**
 *
 * Advice for specific search exceptions
 *
 * @author Sylvain Vissiere-Guerinet
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(0)
public class AccessSearchControllerAdvice {

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ServerErrorResponse> httpClientErrorException(final HttpClientErrorException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ServerErrorResponse(exception.getResponseBodyAsString(), exception));
    }
}
