package fr.cnes.regards.framework.s3.domain;

public interface StorageCommand {

    static Check check(StorageConfig config, StorageCommandID cmdId, String path) {
        return new Check.Impl(config, cmdId, path);
    }

    static Read read(StorageConfig config, StorageCommandID cmdId, String path) {
        return new Read.Impl(config, cmdId, path);
    }

    static Write write(StorageConfig config, StorageCommandID cmdId, String path, StorageEntry entry) {
        return new Write.Impl(config, cmdId, path, entry);
    }

    static Delete delete(StorageConfig config, StorageCommandID cmdId, String path) {
        return new Delete.Impl(config, cmdId, path);
    }

    StorageConfig getConfig();

    String getEntryKey();

    StorageCommandID getCmdId();

    interface Check extends StorageCommand {

        class Impl extends Base implements Check {

            public Impl(StorageConfig config, StorageCommandID cmdId, String path) {
                super(config, cmdId, path);
            }
        }
    }

    interface Read extends StorageCommand {

        class Impl extends Base implements Read {

            public Impl(StorageConfig config, StorageCommandID cmdId, String path) {
                super(config, cmdId, path);
            }
        }
    }

    interface Write extends StorageCommand {

        StorageEntry getEntry();

        class Impl extends Base implements Write {

            private final StorageEntry entry;

            public Impl(StorageConfig config, StorageCommandID cmdId, String path, StorageEntry entry) {
                super(config, cmdId, path);
                this.entry = entry;
            }

            @Override
            public StorageEntry getEntry() {
                return entry;
            }
        }
    }

    /**
     * Delete the content of the archive part at the given storage URI.
     */
    interface Delete extends StorageCommand {

        class Impl extends Base implements Delete {

            public Impl(StorageConfig config, StorageCommandID cmdId, String path) {
                super(config, cmdId, path);
            }
        }
    }

    abstract class Base implements StorageCommand {

        private final StorageCommandID cmdId;

        private final String entryKey;

        private final StorageConfig config;

        private Base(StorageConfig config, StorageCommandID cmdId, String entryKey) {
            this.cmdId = cmdId;
            this.entryKey = entryKey;
            this.config = config;
        }

        @Override
        public String getEntryKey() {
            return entryKey;
        }

        @Override
        public StorageCommandID getCmdId() {
            return cmdId;
        }

        @Override
        public StorageConfig getConfig() {
            return config;
        }
    }

}

