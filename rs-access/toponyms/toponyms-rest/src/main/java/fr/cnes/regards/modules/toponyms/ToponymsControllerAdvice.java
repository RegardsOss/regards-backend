package fr.cnes.regards.modules.toponyms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;
import fr.cnes.regards.modules.toponyms.service.exceptions.GeometryNotHandledException;
import fr.cnes.regards.modules.toponyms.service.exceptions.GeometryNotProcessedException;
import fr.cnes.regards.modules.toponyms.service.exceptions.MaxLimitPerDayException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Advice for specific toponyms exceptions
 *
 * @author Iliana Ghazali
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(0)
public class ToponymsControllerAdvice {

    /**
     * Http Codes corresponding to the exception caught
     */
    private static final Map<String, HttpStatus> EXCEPTION_CODES = ImmutableMap.<String, HttpStatus>builder()
            .put(EntityNotFoundException.class.getName(), HttpStatus.NOT_FOUND)
            .put(GeometryNotProcessedException.class.getName(), HttpStatus.BAD_REQUEST)
            .put(JsonProcessingException.class.getName(), HttpStatus.BAD_REQUEST)
            .put(GeometryNotHandledException.class.getName(), HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .put(MaxLimitPerDayException.class.getName(), HttpStatus.TOO_MANY_REQUESTS).build();

    // --- HANDLING GET EXCEPTIONS ---

    /**
     * Exception handler returning the code 404 when the resource requested was not found
     *
     * @param exception {@link EntityNotFoundException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ServerErrorResponse> entityNotFoundException(final EntityNotFoundException exception) {
        return ResponseEntity.status(getHttpStatus(exception.getClass().getName())).body(buildError(exception));
    }

    // --- HANDLING POST EXCEPTIONS ---

    /**
     * Exception handler when an error occurs while creating a toponym
     *
     * @param exception {@link ModuleException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(ModuleException.class)
    public ResponseEntity<ServerErrorResponse> moduleException(final ModuleException exception) {
        return ResponseEntity.status(getHttpStatus(exception.getClass().getName())).body(buildError(exception));
    }

    /**
     * Exception handler when an error occurs while parsing the feature as a json object
     *
     * @param exception {@link JsonProcessingException}
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ServerErrorResponse> jsonProcessingException(final JsonProcessingException exception) {
        return ResponseEntity.status(getHttpStatus(exception.getClass().getName())).body(buildError(exception));
    }

    // --- UTILS ---

    private ServerErrorResponse buildError(Exception exception) {
        String message = exception.getMessage();
        if (exception.getCause() != null) {
            message += " Cause: " + exception.getCause().getMessage();
        }
        return new ServerErrorResponse(message, exception);
    }

    private HttpStatus getHttpStatus(String className) {
        return EXCEPTION_CODES.getOrDefault(className, HttpStatus.BAD_REQUEST);
    }
}

