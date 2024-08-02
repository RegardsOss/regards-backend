/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.utils;

import io.vavr.CheckedRunnable;
import reactor.core.publisher.Mono;

/**
 * A class with no information, with only a single instance ({@link Unit#UNIT}).
 * <br/>
 * Whereas {@link Void} is supposed to have no instance, and thus no returned value
 * (even though null is required to be used as a substitute), Unit is supposed to have
 * a single non-null value with no information: it returns something, a "proof" that the computation
 * occurred normally.
 * <br/>
 * It is still possible to use null as a secondary instance of Unit, but it must never be done.
 *
 * @author gandrieu
 */
public class Unit {

    // Only instance of Unit.
    private static final Unit UNIT = new Unit();

    private Unit() {
    }

    public static Unit unit() {
        return UNIT;
    }

    public static <T> Unit forget(T t) {
        return UNIT;
    }

    public static Mono<Unit> mono() {
        return Mono.just(UNIT);
    }

    public static <T> Mono<Unit> forgetMono(T t) {
        return Mono.just(UNIT);
    }

    public static Mono<Unit> fromCallable(CheckedRunnable r) {
        return Mono.fromCallable(() -> {
            r.unchecked().run();
            return UNIT;
        });
    }
}
