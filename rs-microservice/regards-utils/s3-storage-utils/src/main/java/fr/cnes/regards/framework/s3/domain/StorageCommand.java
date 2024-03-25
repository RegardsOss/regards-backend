package fr.cnes.regards.framework.s3.domain;

import fr.cnes.regards.framework.s3.dto.StorageConfigDto;

public interface StorageCommand {

    static Check check(StorageConfigDto config, StorageCommandID cmdId, String path) {
        return new Check.Impl(config, cmdId, path);
    }

    static Read read(StorageConfigDto config, StorageCommandID cmdId, String path) {
        return new Read.Impl(config, cmdId, path);
    }

    static Write write(StorageConfigDto config, StorageCommandID cmdId, String path, StorageEntry entry) {
        return new Write.Impl(config, cmdId, path, entry);
    }

    static Write write(StorageConfigDto config,
                       StorageCommandID cmdId,
                       String path,
                       StorageEntry entry,
                       String checksum) {
        return new Write.Impl(config, cmdId, path, entry, checksum);
    }

    static Delete delete(StorageConfigDto config, StorageCommandID cmdId, String path) {
        return new Delete.Impl(config, cmdId, path);
    }

    StorageConfigDto getConfig();

    String getEntryKey();

    StorageCommandID getCmdId();

    interface Check extends StorageCommand {

        class Impl extends Base implements Check {

            public Impl(StorageConfigDto config, StorageCommandID cmdId, String path) {
                super(config, cmdId, path);
            }
        }
    }

    interface Read extends StorageCommand {

        class Impl extends Base implements Read {

            public Impl(StorageConfigDto config, StorageCommandID cmdId, String path) {
                super(config, cmdId, path);
            }
        }
    }

    interface Write extends StorageCommand {

        StorageEntry getEntry();

        String getChecksum();

        class Impl extends Base implements Write {

            private final StorageEntry entry;

            private final String checksum;

            public Impl(StorageConfigDto config, StorageCommandID cmdId, String path, StorageEntry entry) {
                super(config, cmdId, path);
                this.entry = entry;
                this.checksum = null;
            }

            public Impl(StorageConfigDto config,
                        StorageCommandID cmdId,
                        String path,
                        StorageEntry entry,
                        String checksum) {
                super(config, cmdId, path);
                this.entry = entry;
                this.checksum = checksum;
            }

            @Override
            public StorageEntry getEntry() {
                return entry;
            }

            @Override
            public String getChecksum() {
                return checksum;
            }
        }
    }

    /**
     * Delete the content of the archive part at the given storage URI.
     */
    interface Delete extends StorageCommand {

        class Impl extends Base implements Delete {

            public Impl(StorageConfigDto config, StorageCommandID cmdId, String path) {
                super(config, cmdId, path);
            }
        }
    }

    abstract class Base implements StorageCommand {

        private final StorageCommandID cmdId;

        private final String entryKey;

        private final StorageConfigDto config;

        private Base(StorageConfigDto config, StorageCommandID cmdId, String entryKey) {
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
        public StorageConfigDto getConfig() {
            return config;
        }
    }

}

