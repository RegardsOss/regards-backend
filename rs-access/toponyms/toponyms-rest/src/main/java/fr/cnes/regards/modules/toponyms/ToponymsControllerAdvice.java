package fr.cnes.regards.modules.toponyms;

import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;
import fr.cnes.regards.modules.toponyms.service.exceptions.GeometryNotParsedException;
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
 * @author Iliana Ghazali
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(0)
public class ToponymsControllerAdvice {

    /**
     * Exception handler returning the code 400 when an error occurs while processing parsing toponym.
     * @param exception {@link GeometryNotParsedException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(GeometryNotParsedException.class)
    public ResponseEntity<ServerErrorResponse> geometryNotParsedException(final GeometryNotParsedException exception) {
        String message = exception.getMessage();
        if (exception.getCause() != null) {
            message += ". Cause: " + exception.getCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponse(message, exception));
    }

    /**
     * Exception handler returning the code 403 when a user has reached the maximum number of toponyms added in a day
     * @param exception {@link MaxLimitPerDayException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(MaxLimitPerDayException.class)
    public ResponseEntity<ServerErrorResponse> maxLimitPerDayException(final MaxLimitPerDayException exception) {
        String message = exception.getMessage();
        if (exception.getCause() != null) {
            message += ". Cause: " + exception.getCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ServerErrorResponse(message, exception));
    }
}

