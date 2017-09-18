/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.domain;

import java.nio.file.Path;
import java.util.SortedSet;

import com.google.common.collect.Sets;

/**
 * Class to represent a file stored in STAF in multiple parts.
 * Exemple : file.txt -> stored as file.txt_00, file.txt_01, file.txt_02.
 *
 * @author Sébastien Binda
 *
 */
public class PhysicalCutFile extends AbstractPhysicalFile {

    /**
     * {@link Path} of the local uncuted file to store.
     */
    private Path localFile;

    /**
     * {@link SortedSet} of {@link PhysicalCutPartFile} corresponding of the local part files to store.
     */
    private final SortedSet<PhysicalCutPartFile> cutFileParts = Sets.newTreeSet(this::physicalCutPartFileComparator);

    /**
     * Constructor
     * @param pLocalFile {@link Pat} of the local uncuted file to store.
     * @param pSTAFArchiveName {@link String} Name of the STAF Archive where to store the file.
     * @param pSTAFNode {@link Path} STAF Node where to store the file.
     * @param pSTAFFileName {@link String} STAF File name
     */
    public PhysicalCutFile(Path pLocalFile, String pSTAFArchiveName, Path pSTAFNode, String pSTAFFileName) {
        super(STAFArchiveModeEnum.CUT, pSTAFArchiveName, pSTAFNode, pSTAFFileName, PhysicalFileStatusEnum.PENDING);
        localFile = pLocalFile;
    }

    @Override
    public Path getLocalFilePath() {
        return localFile;
    }

    @Override
    public void setLocalFilePath(Path pLocalFilePath) {
        localFile = pLocalFilePath;
    }

    public SortedSet<PhysicalCutPartFile> getCutedFileParts() {
        return cutFileParts;
    }

    public void addCutedPartFile(PhysicalCutPartFile pCutPartFile) {
        cutFileParts.add(pCutPartFile);
    }

    /**
     * Comparator for the {@link SortedSet} of the physical cut parts.
     * @param pF1 {@link PhysicalCutPartFile}
     * @param pF2 {@link PhysicalCutPartFile}
     * @return 0 if equals. -1 if pF1 < pF2. 1 if pF1 > pF2.
     */
    public int physicalCutPartFileComparator(PhysicalCutPartFile pF1, PhysicalCutPartFile pF2) {
        if (pF1.getPartIndex() < pF2.getPartIndex()) {
            return -1;
        }
        if (pF1.getPartIndex() > pF2.getPartIndex()) {
            return 1;
        }
        return 0;
    }

}
