package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;

import com.google.common.collect.Maps;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

public class PhysicalTARFile extends AbstractPhysicalFile {

    /**
     * Map to assciate raw files to add into the STAF System and prepared files into the TAR directory used to create the TAR file.
     *
     * Key : file in tar path (file added in the tar file stored in STAF)
     * value : raw file path (raw file to archive)
     */
    private final Map<Path, Path> filesInTar = Maps.newHashMap();

    /**
     * {@link Path} to the local TAR file to store into STAF system.
     */
    private Path localTarFile;

    /**
     * {@link Path} to the local directory containing files to TAR
     */
    private Path localTarDirectory;

    /**
     * {@link LocalDateTime} creation date of the TAR directory.
     */
    private LocalDateTime localTarDirectoryCreationDate;

    public PhysicalTARFile(String pSTAFArchiveName, String pSTAFNode) {
        super(STAFArchiveModeEnum.TAR, pSTAFArchiveName, pSTAFNode, PhysicalFileStatusEnum.PENDING);
    }

    public Map<Path, Path> getFilesInTar() {
        return filesInTar;
    }

    public Path getLocalTarFile() {
        return localTarFile;
    }

    public void setLocalTarFile(Path pLocalTarFile) {
        localTarFile = pLocalTarFile;
    }

    public void addFileInTar(Path pFileInTar, Path pRawFile) {
        filesInTar.put(pFileInTar, pRawFile);
    }

    public Path getLocalTarDirectory() {
        return localTarDirectory;
    }

    public void setLocalTarDirectory(Path pLocalTarDirectory) {
        localTarDirectory = pLocalTarDirectory;
    }

    public LocalDateTime getLocalTarDirectoryCreationDate() {
        return localTarDirectoryCreationDate;
    }

    public void setLocalTarDirectoryCreationDate(LocalDateTime pLocalTarDirectoryCreationDate) {
        localTarDirectoryCreationDate = pLocalTarDirectoryCreationDate;
    }

    @Override
    public Path getLocalFilePath() {
        return localTarFile;
    }

    @Override
    public Path getSTAFFilePath() throws STAFException {
        if (localTarFile != null) {
            return Paths.get(super.getStafNode(), localTarFile.getFileName().toString());
        } else {
            throw new STAFException("[STAF] Error during STAF PATH creation");
        }
    }

}