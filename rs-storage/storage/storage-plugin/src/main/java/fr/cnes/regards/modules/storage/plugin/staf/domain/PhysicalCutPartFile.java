package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.nio.file.Path;
import java.nio.file.Paths;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

public class PhysicalCutPartFile extends AbstractPhysicalFile {

    /**
     * Local cuted part file to store
     */
    private final Path localFilePath;

    /**
     * AbstractPhysicalFile type CUT including all cuted part of the original file.
     */
    private final PhysicalCutFile includingCutFile;

    /**
     * Index of the current cut part file.
     */
    private final int partIndex;

    public PhysicalCutPartFile(Path pLocalFile, Path pRawFilePath, PhysicalCutFile pIncludingCutFile, int pPartIndex,
            String pSTAFArchiveName, String pSTAFNode) {
        super(STAFArchiveModeEnum.CUT_PART, pSTAFArchiveName, pSTAFNode, PhysicalFileStatusEnum.TO_STORE);
        localFilePath = pLocalFile;
        includingCutFile = pIncludingCutFile;
        partIndex = pPartIndex;
        super.addRawAssociatedFile(pRawFilePath);
    }

    public PhysicalCutFile getIncludingCutFile() {
        return includingCutFile;
    }

    public int getPartIndex() {
        return partIndex;
    }

    @Override
    public Path getLocalFilePath() {
        return localFilePath;
    }

    @Override
    public Path getSTAFFilePath() {
        // The file path of the stored cuted part file is not calculated with the md5 of the part
        // but with the md5 of the global file with index prefix.
        return Paths.get(String.format("%s_%d", includingCutFile.getSTAFFilePath(), partIndex));
    }

}
