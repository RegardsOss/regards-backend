package fr.cnes.regards.modules.processing.domain.constraints;

import io.vavr.CheckedFunction1;
import io.vavr.Value;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Try;
import reactor.core.publisher.Mono;

public interface ConstraintChecker<T> {

    Mono<Seq<Violation>> validate(T value);

    default ConstraintChecker<T> and(ConstraintChecker<T> other) {
        return t -> validate(t).flatMap(vs -> other.validate(t).map(os -> vs.appendAll(os)));
    }


    static <T> ConstraintChecker<T> and(CheckedFunction1<T, Value<Violation>> fn) {
        return t -> Try.of(() -> fromValue(fn.apply(t)))
            .recover(e -> Mono.error(() -> new ExceptionViolation(e)))
            .get();
    }

    static <T> Mono<Seq<T>> fromValue(Value<T> v) {
        return v.map(x -> Mono.<Seq<T>>just(List.of(x))).getOrElse(() -> Mono.empty());
    }

    static <T> ConstraintChecker<T> noViolation() {
        return t ->  Mono.empty();
    }

    static <T> ConstraintChecker<T> violation(Violation v) {
        return t ->  Mono.just(List.of(v));
    }

}
