package fr.cnes.regards.modules.storage.plugin.staf.domain;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

public abstract class STAFPhysicalFile {

    /**
     * STAF Archiving mode TAR|CUT|NORMAL {@link STAFArchiveModeEnum}
     */
    private final STAFArchiveModeEnum archiveMode;

    private final String stafNode;

    public STAFPhysicalFile(STAFArchiveModeEnum pArchiveMode, String pSTAFNode) {
        super();
        archiveMode = pArchiveMode;
        stafNode = pSTAFNode;
    }

    public STAFArchiveModeEnum getArchiveMode() {
        return archiveMode;
    }

}
