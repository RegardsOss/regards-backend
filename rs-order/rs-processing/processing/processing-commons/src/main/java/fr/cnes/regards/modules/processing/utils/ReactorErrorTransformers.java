package fr.cnes.regards.modules.processing.utils;

import fr.cnes.regards.modules.processing.exceptions.ProcessingException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface ReactorErrorTransformers {

    static <K, V> Function<Context, Context> addInContext(K key, V value) {
        return ctx -> ctx.put(key, value);
    }

    static <K1, V1, K2, V2> Function<Context, Context> addInContext(K1 key1, V1 value1, K2 key2, V2 value2) {
        return ctx -> ctx.put(key1, value1).put(key2, value2);
    }

    static <K1, V1, K2, V2, K3, V3> Function<Context, Context> addInContext(K1 key1, V1 value1, K2 key2, V2 value2,
            K3 key3, V3 value3) {
        return ctx -> ctx.put(key1, value1).put(key2, value2).put(key3, value3);
    }

    static <K, T, E extends ProcessingException> Function<Throwable, Mono<T>> errorWithContextMono(Class<K> key,
            BiFunction<K, Throwable, E> fn) {
        return t -> Mono.subscriberContext()
            .map(ctx -> ctx.get(key))
            .flatMap(k -> Mono.<T>error(fn.apply(k, t)));
    }

}
