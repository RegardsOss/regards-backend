/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.processing.domain.constraints;

import io.vavr.CheckedFunction1;
import io.vavr.Value;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Try;
import reactor.core.publisher.Mono;

/**
 * This interface defines how to check for constraints attached to a process.
 *
 * @author gandrieu
 */
public interface ConstraintChecker<T> {

    Mono<Seq<Violation>> validate(T value);

    default ConstraintChecker<T> and(ConstraintChecker<T> other) {
        return t -> validate(t).flatMap(vs -> other.validate(t).map(vs::appendAll));
    }

    static <T> ConstraintChecker<T> and(CheckedFunction1<T, Value<Violation>> fn) {
        return t -> Try.of(() -> fromValue(fn.apply(t)))
                       .recover(e -> Mono.error(() -> new ExceptionViolation(e)))
                       .get();
    }

    static <T> Mono<Seq<T>> fromValue(Value<T> v) {
        return v.map(x -> Mono.<Seq<T>>just(List.of(x))).getOrElse(Mono::empty);
    }

    static <T> ConstraintChecker<T> noViolation() {
        return t -> Mono.empty();
    }

    static <T> ConstraintChecker<T> violation(Violation v) {
        return t -> Mono.just(List.of(v));
    }

}
