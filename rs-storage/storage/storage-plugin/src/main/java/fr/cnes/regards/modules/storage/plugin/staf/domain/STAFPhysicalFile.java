package fr.cnes.regards.modules.storage.plugin.staf.domain;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

public abstract class STAFPhysicalFile {

    /**
     * STAF Archiving mode TAR|CUT|NORMAL {@link STAFArchiveModeEnum}
     */
    private final STAFArchiveModeEnum archiveMode;

    public STAFPhysicalFile(STAFArchiveModeEnum pArchiveMode) {
        super();
        archiveMode = pArchiveMode;
    }

    public STAFArchiveModeEnum getArchiveMode() {
        return archiveMode;
    }

}
