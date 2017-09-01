package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.nio.file.Path;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

public class PhysicalFile extends STAFPhysicalFile {

    private final Path localFile;

    private final String stafFileName;

    public PhysicalFile(String pSTAFNode, Path pLocalFile, String pSTAFFileName) {
        super(STAFArchiveModeEnum.NORMAL, pSTAFNode);
        localFile = pLocalFile;
        stafFileName = pSTAFFileName;
    }

    public Path getLocalFile() {
        return localFile;
    }

    public String getStafFileName() {
        return stafFileName;
    }

}
