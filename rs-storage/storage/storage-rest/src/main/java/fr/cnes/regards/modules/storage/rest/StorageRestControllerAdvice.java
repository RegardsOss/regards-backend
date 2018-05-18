package fr.cnes.regards.modules.storage.rest;

import java.nio.file.NoSuchFileException;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(0)
public class StorageRestControllerAdvice {

    @ExceptionHandler(NoSuchFileException.class)
    public ResponseEntity<ServerErrorResponse> entityNotFound(final NoSuchFileException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ServerErrorResponse(exception.getMessage()));
    }

}
