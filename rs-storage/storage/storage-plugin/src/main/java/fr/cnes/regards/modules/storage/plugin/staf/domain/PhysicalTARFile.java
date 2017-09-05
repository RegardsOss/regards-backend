package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import fr.cnes.regards.framework.file.utils.ChecksumUtils;
import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

public class PhysicalTARFile extends AbstractPhysicalFile {

    private String stafFileName;

    private final Set<Path> localFiles;

    private Path localTarFile;

    public PhysicalTARFile(String pSTAFNode, Set<Path> pLocalFiles) {
        super(STAFArchiveModeEnum.TAR, pSTAFNode, PhysicalFileStatusEnum.PENDING);
        localFiles = pLocalFiles;
    }

    public String getStafFileName() {
        return stafFileName;
    }

    public Set<Path> getLocalFiles() {
        return localFiles;
    }

    public Path getLocalTarFile() {
        return localTarFile;
    }

    public void setLocalTarFile(Path pLocalTarFile) {
        localTarFile = pLocalTarFile;
    }

    @Override
    public Path getLocalFilePath() {
        return localTarFile;
    }

    @Override
    public Path getSTAFFilePath() {
        // If staf file path is already calculed return it
        if (stafFileName != null) {
            return Paths.get(super.getStafNode(), stafFileName);
        } else if ((localTarFile != null) && localTarFile.toFile().exists() && localTarFile.toFile().canRead()) {
            // Else, calculate staf file name with the current date
            try (FileInputStream is = new FileInputStream(localTarFile.toFile())) {
                stafFileName = ChecksumUtils.computeHexChecksum(is, "md5");
                return Paths.get(super.getStafNode(), stafFileName);
            } catch (IOException | NoSuchAlgorithmException e) {
                LOG.error("Error calculating file checksum {}", localTarFile.toString(), e);
                return null;
            }
        } else {
            return null;
        }
    }

}