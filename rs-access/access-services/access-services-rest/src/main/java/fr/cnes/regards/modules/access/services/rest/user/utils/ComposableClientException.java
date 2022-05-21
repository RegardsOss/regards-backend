package fr.cnes.regards.modules.access.services.rest.user.utils;

import io.vavr.collection.Seq;
import io.vavr.control.Option;

public abstract class ComposableClientException extends Exception {

    private ComposableClientException() {
        super();
    }

    private ComposableClientException(Throwable cause) {
        super(cause);
    }

    public static final ComposableClientException EMPTY = new Empty();

    public static class Empty extends ComposableClientException {

        Empty() {
            super();
        }

        @Override
        public Seq<Throwable> causes() {
            return io.vavr.collection.List.of();
        }
    }

    public static class Single extends ComposableClientException {

        Single(Throwable cause) {
            super(cause == null ? new Exception() : cause);
        }

        @Override
        public Seq<Throwable> causes() {
            Throwable cause = getCause();
            return (cause instanceof ComposableClientException) ?
                ((ComposableClientException) cause).causes() :
                io.vavr.collection.List.of(cause);
        }
    }

    public static class Multiple extends ComposableClientException {

        private final Seq<Throwable> causes;

        Multiple(Seq<Throwable> causes) {
            super();
            this.causes = Option.of(causes).getOrElse(io.vavr.collection.List.empty());
        }

        @Override
        public Seq<Throwable> causes() {
            return causes.flatMap(cause -> (cause instanceof ComposableClientException) ?
                ((ComposableClientException) cause).causes() :
                io.vavr.collection.List.of(cause));
        }
    }

    public abstract Seq<Throwable> causes();

    public ComposableClientException compose(ComposableClientException other) {
        if (other == null) {
            return this;
        }
        return new Multiple(this.causes().appendAll(other.causes()));
    }

    public static ComposableClientException make(Throwable cause) {
        return new Single(cause);
    }
}
