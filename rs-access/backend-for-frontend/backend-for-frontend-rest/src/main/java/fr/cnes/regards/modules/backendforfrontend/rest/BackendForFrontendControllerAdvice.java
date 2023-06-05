package fr.cnes.regards.modules.backendforfrontend.rest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Advice for back-for-frontend exceptions
 *
 * @author Sylvain Vissiere-Guerinet
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(0)
public class BackendForFrontendControllerAdvice {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendForFrontendControllerAdvice.class);

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<JsonObject> httpClientErrorException(final HttpStatusCodeException exception) {
        return buildError(exception);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<JsonObject> httpServerErrorException(final HttpServerErrorException exception) {
        return buildError(exception);
    }

    private ResponseEntity<JsonObject> buildError(HttpStatusCodeException exception) {
        LOGGER.error(exception.getMessage(), exception);
        JsonObject jsonObject = JsonParser.parseString(exception.getResponseBodyAsString()).getAsJsonObject();
        return ResponseEntity.status(exception.getStatusCode()).body(jsonObject);
    }
}
