/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.domain;

import java.nio.file.Path;

import org.apache.commons.lang.StringUtils;

import fr.cnes.regards.framework.staf.exception.STAFException;

/**
 * Class to represent one of the parts of a file stored in STAF as multiple parts.
 * @author Sébastien Binda
 *
 */
public class PhysicalCutPartFile extends AbstractPhysicalFile {

    /**
     * Local cuted part file to store
     */
    private Path localFilePath;

    /**
     * AbstractPhysicalFile type CUT including all cuted part of the original file.
     */
    private final PhysicalCutFile includingCutFile;

    /**
     * Index of the current cut part file.
     */
    private final int partIndex;

    /**
     * Constructor
     * @param pLocalFile {@link Path} Local file representing one part of the including uncuted file.
     * @param pIncludingCutFile {@link PhysicalCutFile} Including global file wher all parts of the uncuted file are listed.
     * @param pPartIndex Index of the current part
     * @param pSTAFArchiveName {@link String} STAF Archive name where the part is stored.
     * @param pSTAFNode {@link Path} STAF Node where the part is stored.
     * @param pSTAFFileName {@link String} STAF File name
     */
    public PhysicalCutPartFile(Path pLocalFile, PhysicalCutFile pIncludingCutFile, int pPartIndex,
            String pSTAFArchiveName, Path pSTAFNode, String pSTAFFileName) {
        super(STAFArchiveModeEnum.CUT_PART, pSTAFArchiveName, pSTAFNode, pSTAFFileName,
              PhysicalFileStatusEnum.TO_STORE);
        localFilePath = pLocalFile;
        includingCutFile = pIncludingCutFile;
        partIndex = pPartIndex;
        pIncludingCutFile.getRawAssociatedFiles().stream().forEach(super::addRawAssociatedFile);
    }

    @Override
    public Path getLocalFilePath() {
        return localFilePath;
    }

    @Override
    public void setLocalFilePath(Path pLocalFilePath) {
        localFilePath = pLocalFilePath;
    }

    public static String calculatePartFileName(String pFullFileName, int pPartIndex) throws STAFException {
        return String.format("%s_%s", pFullFileName, StringUtils.leftPad(String.valueOf(pPartIndex), 2, "0"));
    }

    public PhysicalCutFile getIncludingCutFile() {
        return includingCutFile;
    }

    public int getPartIndex() {
        return partIndex;
    }

}
