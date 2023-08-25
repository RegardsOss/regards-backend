package fr.cnes.regards.framework.s3.domain;

import io.vavr.Function1;
import reactor.core.publisher.Mono;

public interface StorageCommandResult {

    StorageCommand s3Cmd();

    interface CheckResult extends StorageCommandResult {

        default <R> R matchCheckResult(Function1<? super CheckPresent, R> present,
                                       Function1<? super CheckAbsent, R> absent,
                                       Function1<? super UnreachableStorage, R> unreachable) {
            if (this instanceof UnreachableStorage) {
                return unreachable.apply((UnreachableStorage) this);
            } else if (this instanceof CheckPresent) {
                return present.apply((CheckPresent) this);
            } else if (this instanceof CheckAbsent) {
                return absent.apply((CheckAbsent) this);
            } else {
                throw new IllegalStateException("No matching function for this " + this);
            }
        }
    }

    interface ReadResult extends StorageCommandResult {

        default <R> R matchReadResult(Function1<? super ReadingPipe, R> pipe,
                                      Function1<? super UnreachableStorage, R> unreachable,
                                      Function1<? super ReadNotFound, R> notFound) {
            if (this instanceof UnreachableStorage) {
                return unreachable.apply((UnreachableStorage) this);
            } else if (this instanceof ReadingPipe) {
                return pipe.apply((ReadingPipe) this);
            } else if (this instanceof ReadNotFound) {
                return notFound.apply((ReadNotFound) this);
            } else {
                throw new IllegalStateException("No matching function for this " + this);
            }
        }
    }

    interface WriteResult extends StorageCommandResult {

        default <R> R matchWriteResult(Function1<? super WriteSuccess, R> success,
                                       Function1<? super UnreachableStorage, R> unreachable,
                                       Function1<? super WriteFailure, R> failure) {
            if (this instanceof UnreachableStorage) {
                return unreachable.apply((UnreachableStorage) this);
            } else if (this instanceof WriteSuccess) {
                return success.apply((WriteSuccess) this);
            } else if (this instanceof WriteFailure) {
                return failure.apply((WriteFailure) this);
            } else {
                throw new IllegalStateException("No matching function for this " + this);
            }
        }
    }

    interface DeleteResult extends StorageCommandResult {

        default <R> R matchDeleteResult(Function1<? super DeleteSuccess, R> success,
                                        Function1<? super UnreachableStorage, R> unreachable,
                                        Function1<? super DeleteFailure, R> failure) {
            if (this instanceof UnreachableStorage) {
                return unreachable.apply((UnreachableStorage) this);
            } else if (this instanceof DeleteSuccess) {
                return success.apply((DeleteSuccess) this);
            } else if (this instanceof DeleteFailure) {
                return failure.apply((DeleteFailure) this);
            } else {
                throw new IllegalStateException("No matching function for this " + this);
            }
        }
    }

    //=== CHECK RESULTS
    class CheckPresent extends Base implements CheckResult {

        public CheckPresent(StorageCommand ioCmd) {
            super(ioCmd);
        }
    }

    class CheckAbsent extends Base implements CheckResult {

        public CheckAbsent(StorageCommand ioCmd) {
            super(ioCmd);
        }
    }

    //=== READ RESULTS
    class ReadNotFound extends Base implements ReadResult {

        public ReadNotFound(StorageCommand ioCmd) {
            super(ioCmd);
        }
    }

    class ReadingPipe extends Base implements ReadResult {

        private final Mono<StorageEntry> entry;

        public ReadingPipe(StorageCommand ioCmd, Mono<StorageEntry> entry) {
            super(ioCmd);
            this.entry = entry;
        }

        public Mono<StorageEntry> getEntry() {
            return entry;
        }
    }

    //=== WRITE RESULTS

    class WriteSuccess extends Base implements WriteResult {

        private final long size;

        private final String checksum;

        public WriteSuccess(StorageCommand ioCmd, long size, String checksum) {
            super(ioCmd);
            this.size = size;
            this.checksum = checksum;
        }

        public long getSize() {
            return size;
        }

        public String getChecksum() {
            return checksum;
        }
    }

    /**
     * Signifies that writing failed at the beginning,
     * or that written data has been cleaned up.
     */
    class WriteFailure extends Base implements WriteResult {

        private Throwable cause;

        public WriteFailure(StorageCommand ioCmd, Throwable cause) {
            super(ioCmd);
            this.cause = cause;
        }

        public Throwable getCause() {
            return cause;
        }
    }

    //=== DELETE RESULTS

    class DeleteSuccess extends Base implements DeleteResult {

        public DeleteSuccess(StorageCommand ioCmd) {
            super(ioCmd);
        }
    }

    /**
     * Signifies that deleting failed at the beginning.
     */
    class DeleteFailure extends Base implements DeleteResult {

        public DeleteFailure(StorageCommand ioCmd) {
            super(ioCmd);
        }
    }

    //=== COMMON FAILURES

    class UnreachableStorage extends Base implements CheckResult, ReadResult, WriteResult, DeleteResult {

        private Throwable throwable;

        public UnreachableStorage(StorageCommand ioCmd, Throwable throwable) {
            super(ioCmd);
            this.throwable = throwable;
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }

    abstract class Base implements StorageCommandResult {

        private final StorageCommand s3Cmd;

        protected Base(StorageCommand s3Cmd) {
            this.s3Cmd = s3Cmd;
        }

        @Override
        public StorageCommand s3Cmd() {
            return s3Cmd;
        }
    }

}

