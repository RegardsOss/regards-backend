package fr.cnes.regards.modules.access.services.rest.user.utils;

import io.vavr.control.Either;
import io.vavr.control.Validation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.util.function.Function;

public class Try {

    private Try() {}

    public static  <T> Function<io.vavr.control.Try<ResponseEntity<T>>, Validation<ComposableClientException, T>> handleClientFailure(String clientName) {
        return (t) -> t
            .toEither()
            .mapLeft(ComposableClientException::make)
            .flatMap(response -> {
                if (!response.getStatusCode().is2xxSuccessful()) {
                    return Either.left(
                        ComposableClientException.make(
                            new RestClientException(String.format("Request to %s failed with status %s.", clientName, response.getStatusCodeValue()))
                        )
                    );
                }
                return Either.right(response);
            })
            .map(ResponseEntity::getBody)
            .toValidation();
    }
}
