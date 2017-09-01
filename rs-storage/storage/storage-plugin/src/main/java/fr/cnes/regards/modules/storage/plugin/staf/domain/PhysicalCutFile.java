package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.nio.file.Path;
import java.util.Set;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

public class PhysicalCutFile extends STAFPhysicalFile {

    private final Path localFile;

    private final Set<PhysicalFile> cutedFiles;

    public PhysicalCutFile(String pSTAFNode, Path pLocalFile, Set<PhysicalFile> pCutedFiles) {
        super(STAFArchiveModeEnum.CUT, pSTAFNode);
        localFile = pLocalFile;
        cutedFiles = pCutedFiles;
    }

    public Path getLocalFile() {
        return localFile;
    }

    public Set<PhysicalFile> getCutedFiles() {
        return cutedFiles;
    }

}
