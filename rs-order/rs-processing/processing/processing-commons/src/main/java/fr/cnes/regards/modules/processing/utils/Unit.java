package fr.cnes.regards.modules.processing.utils;

import io.vavr.CheckedRunnable;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

/**
 * A class with no information, with only a single instance ({@link Unit#UNIT}).
 * <br/>
 * Whereas {@link Void} is supposed to have no instance, and thus no returned value
 * (even though null is required to be used as a substitute), Unit is supposed to have
 * a single non-null value with no information: it returns something, a "proof" that the computation
 * occurred normally.
 * <br/>
 * It is still possible to use null as a secondary instance of Unit, but it must never be done.
 */
public class Unit {

    // Only instance of Unit.
    private static final Unit UNIT = new Unit();
    private Unit() {}


    public static Unit unit() { return UNIT; }
    public static <T> Unit forget(T t) { return UNIT; }

    public static Mono<Unit> mono() {return Mono.just(UNIT); }
    public static <T> Mono<Unit> forgetMono(T t) { return Mono.just(UNIT); }

    public static Mono<Unit> fromCallable(CheckedRunnable r) {
        return Mono.fromCallable(() -> {
            r.unchecked().run();
            return UNIT;
        });
    }
}
