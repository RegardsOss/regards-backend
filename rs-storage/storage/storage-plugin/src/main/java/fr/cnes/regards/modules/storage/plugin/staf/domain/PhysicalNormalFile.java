package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import fr.cnes.regards.framework.file.utils.ChecksumUtils;
import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

public class PhysicalNormalFile extends AbstractPhysicalFile {

    private final Path localFile;

    public PhysicalNormalFile(Path pLocalFile, Path pRawFilePath, String pSTAFNode) {
        super(STAFArchiveModeEnum.NORMAL, pSTAFNode, PhysicalFileStatusEnum.TO_STORE);
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
