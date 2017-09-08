/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.file.utils.ChecksumUtils;
import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

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
    private final Path localFile;

    /**
     * {@link Set} of {@link PhysicalCutPartFile} corresponding of the local part files to store.
     */
    private final Set<PhysicalCutPartFile> cutFileParts = Sets.newHashSet();

    /**
     * Constructor
     * @param pLocalFile {@link Pat} of the local uncuted file to store.
     * @param pSTAFArchiveName {@link String} Name of the STAF Archive where to store the file.
     * @param pSTAFNode {@link String} STAF Node where to store the file.
     */
    public PhysicalCutFile(Path pLocalFile, String pSTAFArchiveName, String pSTAFNode) {
        super(STAFArchiveModeEnum.CUT, pSTAFArchiveName, pSTAFNode, PhysicalFileStatusEnum.PENDING);
        localFile = pLocalFile;
    }

    @Override
    public Path getLocalFilePath() {
        return localFile;
    }

    @Override
    public Path getSTAFFilePath() {
        try (FileInputStream is = new FileInputStream(localFile.toFile())) {
            return Paths.get(super.getStafNode(), ChecksumUtils.computeHexChecksum(is, "md5"));
        } catch (IOException | NoSuchAlgorithmException e) {
            LOG.error("Error calculating file checksum {}", localFile.toString(), e);
            return null;
        }
    }

    public Path getLocalFile() {
        return localFile;
    }

    public Set<PhysicalCutPartFile> getCutedFileParts() {
        return cutFileParts;
    }

    public void addCutedPartFile(PhysicalCutPartFile pCutPartFile) {
        cutFileParts.add(pCutPartFile);
    }

}
