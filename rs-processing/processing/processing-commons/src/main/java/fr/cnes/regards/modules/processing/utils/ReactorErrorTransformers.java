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

import fr.cnes.regards.modules.processing.exceptions.ProcessingException;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Various utility functions regarding Reactor context to pass context information
 * (current tenant, user, role, batch, execution, etc.)
 * in the context instead of having to propagate the content in functions signatures.
 *
 * @author gandrieu
 */
public interface ReactorErrorTransformers {

    static <K, V> Function<Context, Context> addInContext(K key, V value) {
        return ctx -> ctx.put(key, value);
    }

    static <K1, V1, K2, V2> Function<Context, Context> addInContext(K1 key1, V1 value1, K2 key2, V2 value2) {
        return ctx -> ctx.put(key1, value1).put(key2, value2);
    }

    static <K1, V1, K2, V2, K3, V3> Function<Context, Context> addInContext(K1 key1,
                                                                            V1 value1,
                                                                            K2 key2,
                                                                            V2 value2,
                                                                            K3 key3,
                                                                            V3 value3) {
        return ctx -> ctx.put(key1, value1).put(key2, value2).put(key3, value3);
    }

    static <K, T, E extends ProcessingException> Function<Throwable, Mono<T>> errorWithContextMono(Class<K> key,
                                                                                                   BiFunction<K, Throwable, E> fn) {
        return t -> Mono.deferContextual(Mono::just)
                        .map(ctx -> ctx.get(key))
                        .flatMap(k -> Mono.<T>error(fn.apply(k, t)));
    }

}
