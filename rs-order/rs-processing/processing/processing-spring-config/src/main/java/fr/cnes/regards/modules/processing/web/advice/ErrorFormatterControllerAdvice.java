package fr.cnes.regards.modules.processing.web.advice;

import fr.cnes.regards.modules.processing.exceptions.ProcessingException;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;

@ControllerAdvice
public class ErrorFormatterControllerAdvice {

    @Value
    static class ErrorStructure {
        UUID errorId;
        String message;
        OffsetDateTime time;
    }

    @ExceptionHandler(ProcessingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ResponseEntity<ErrorStructure> processValidationError(ProcessingException e) {
        return new ResponseEntity<>(
            new ErrorStructure(
                e.getExceptionId(),
                e.getMessage(),
                nowUtc()
            ),
            e.getType().getStatus()
        );
    }
}