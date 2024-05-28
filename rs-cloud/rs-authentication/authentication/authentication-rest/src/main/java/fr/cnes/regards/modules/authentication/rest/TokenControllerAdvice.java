package fr.cnes.regards.modules.authentication.rest;

import fr.cnes.regards.modules.authentication.domain.exception.oauth2.AuthenticationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * To manage authentication exception
 * @author Olivier Rousselot
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.LOWEST_PRECEDENCE - 200)
public class TokenControllerAdvice {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<AuthenticationException> authenticationException(final AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
    }
}
