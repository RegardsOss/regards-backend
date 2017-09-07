/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import fr.cnes.regards.framework.file.utils.ChecksumUtils;
import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

/**
 * Class to represent a file stored in STAF without transformation.
 * @author Sébastien Binda
 */
public class PhysicalNormalFile extends AbstractPhysicalFile {

    /**
     * {@link Path} to the local file to store.
     */
    private final Path localFile;

    /**
     * Constructor
     * @param pLocalFile {@link Path} to the local file to store.
     * @param pRawFilePath {@link Path} to the raw file to store.
     * Can be different with pLocalFile if the raw file is not accessible and has been temporary stored in the staf workspace.
     * @param pSTAFArchiveName {@link String} STAF Archive name where the part is stored.
     * @param pSTAFNode {@link String} STAF Node where the part is stored.
     */
    public PhysicalNormalFile(Path pLocalFile, Path pRawFilePath, String pSTAFArchiveName, String pSTAFNode) {
        super(STAFArchiveModeEnum.NORMAL, pSTAFArchiveName, pSTAFNode, PhysicalFileStatusEnum.TO_STORE);
        localFile = pLocalFile;
        super.addRawAssociatedFile(pRawFilePath);
    }

    public Path getLocalFile() {
        return localFile;
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

}
