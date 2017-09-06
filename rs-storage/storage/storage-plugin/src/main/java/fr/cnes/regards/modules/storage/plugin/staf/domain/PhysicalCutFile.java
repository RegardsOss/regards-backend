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

public class PhysicalCutFile extends AbstractPhysicalFile {

    private final Path localFile;

    private final Set<PhysicalCutPartFile> cutFileParts = Sets.newHashSet();

    public PhysicalCutFile(Path pLocalFile, String pSTAFArchiveName, String pSTAFNode) {
        super(STAFArchiveModeEnum.CUT, pSTAFArchiveName, pSTAFNode, PhysicalFileStatusEnum.PENDING);
        localFile = pLocalFile;
    }

    public Path getLocalFile() {
        return localFile;
    }

    public Set<PhysicalCutPartFile> getCutedFileParts() {
        return cutFileParts;
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

    public void addCutedPartFile(PhysicalCutPartFile pCutPartFile) {
        cutFileParts.add(pCutPartFile);
    }

}
