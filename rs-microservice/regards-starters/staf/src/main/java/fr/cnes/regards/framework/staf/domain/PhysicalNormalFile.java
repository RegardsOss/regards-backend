/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.domain;

import java.nio.file.Path;

/**
 * Class to represent a file stored in STAF without transformation.
 * @author Sébastien Binda
 */
public class PhysicalNormalFile extends AbstractPhysicalFile {

    /**
     * {@link Path} to the local file to store.
     */
    private Path localFile;

    /**
     * Constructor
     * @param pLocalFile {@link Path} to the local file to store.
     * @param pRawFilePath {@link Path} to the raw file to store.
     * Can be different with pLocalFile if the raw file is not accessible and has been temporary stored in the staf workspace.
     * @param pSTAFArchiveName {@link String} STAF Archive name where the part is stored.
     * @param pSTAFNode {@link String} STAF Node where the part is stored.
     * @param pSTAFFileName {@link String} STAF File name
     */
    public PhysicalNormalFile(Path pLocalFile, Path pRawFilePath, String pSTAFArchiveName, Path pSTAFNode,
            String pSTAFFileName) {
        super(STAFArchiveModeEnum.NORMAL, pSTAFArchiveName, pSTAFNode, pSTAFFileName, PhysicalFileStatusEnum.TO_STORE);
        localFile = pLocalFile;
        if (pRawFilePath != null) {
            super.addRawAssociatedFile(pRawFilePath);
        }
    }

    public Path getLocalFile() {
        return localFile;
    }

    @Override
    public Path getLocalFilePath() {
        return localFile;
    }

    @Override
    public void setLocalFilePath(Path pLocalFilePath) {
        localFile = pLocalFilePath;
    }
}
