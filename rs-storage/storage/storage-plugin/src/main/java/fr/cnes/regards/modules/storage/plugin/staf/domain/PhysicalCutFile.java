package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.nio.file.Path;
import java.util.Set;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

public class PhysicalCutFile extends AbstractPhysicalFile {

    private final Path localFile;

    private final Set<PhysicalNormalFile> cutedFiles;

    public PhysicalCutFile(String pSTAFNode, Path pLocalFile, Set<PhysicalNormalFile> pCutedFiles) {
        super(STAFArchiveModeEnum.CUT, pSTAFNode, PhysicalFileStatusEnum.PENDING);
        localFile = pLocalFile;
        cutedFiles = pCutedFiles;
    }

    public Path getLocalFile() {
        return localFile;
    }

    public Set<PhysicalNormalFile> getCutedFiles() {
        return cutedFiles;
    }

    @Override
    public Path getLocalFilePath() {
        return localFile;
    }

    @Override
    public Path getSTAFFilePath() {
        // No STAF location, the STAF locations of the cuted files are read from the cutedFiles
        return null;
    }

}
