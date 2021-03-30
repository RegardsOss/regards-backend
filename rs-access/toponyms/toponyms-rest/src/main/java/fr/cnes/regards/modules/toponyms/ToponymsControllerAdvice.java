package fr.cnes.regards.modules.toponyms;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;
import fr.cnes.regards.modules.toponyms.service.exceptions.GeometryNotHandledException;
import fr.cnes.regards.modules.toponyms.service.exceptions.MaxLimitPerDayException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Advice for specific toponyms exceptions
 *
 * @author Sylvain Vissiere-Guerinet
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(0)
public class ToponymsControllerAdvice {

    /**
     * Exception handler returning the code 400 when an error occurs while processing parsing toponym.<br>
     *
     * @param exception {@link JsonProcessingException}
     * @return {@link ResponseEntity}
     */

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ServerErrorResponse> jsonProcessingException(final JsonProcessingException exception) {
        String message = exception.getMessage();
        if (exception.getCause() != null) {
            message += ". Cause: " + exception.getCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponse(message, exception));
    }

    @ExceptionHandler(GeometryNotHandledException.class)
    public ResponseEntity<ServerErrorResponse> openSearchParseException(final GeometryNotHandledException exception) {
        String message = exception.getMessage();
        if (exception.getCause() != null) {
            message += ". Cause: " + exception.getCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponse(message, exception));
    }

    @ExceptionHandler(MaxLimitPerDayException.class)
    public ResponseEntity<ServerErrorResponse> openSearchParseException(final MaxLimitPerDayException exception) {
        String message = exception.getMessage();
        if (exception.getCause() != null) {
            message += ". Cause: " + exception.getCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ServerErrorResponse(message, exception));
    }
}

