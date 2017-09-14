/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.domain;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Class to represent a list of files stored in the same STAF file into a tar file.
 * @author Sébastien Binda
 */
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

    /**
     * Current size of the TAR file.
     */
    private Long tarSize = 0L;

    /**
     * Constructor
     * @param pSTAFArchiveName {@link String} STAF Archive name where the tar is stored
     * @param pSTAFNode {@link Path} STAF Node where the tar is stored.
     * @param pSTAFFileName {@link String} STAF File name
     */
    public PhysicalTARFile(String pSTAFArchiveName, Path pSTAFNode, String pSTAFFileName) {
        super(STAFArchiveModeEnum.TAR, pSTAFArchiveName, pSTAFNode, pSTAFFileName, PhysicalFileStatusEnum.PENDING);
    }

    /**
     * Return map assciating raw files and prepared STAF file in the current TAR.<br/>
     * <ul>
     * <li><b>Key</b> : file in tar path (file added in the tar file stored in STAF)</li>
     * <li><b>value</b> : raw file path (raw file to archive)</li>
     * </ul>
     */
    public Map<Path, Path> getFilesInTar() {
        return filesInTar;
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

    public Long getTarSize() {
        return tarSize;
    }

    public void setTarSize(Long pTarSize) {
        tarSize = pTarSize;
    }

    @Override
    public Path getLocalFilePath() {
        return localTarFile;
    }

    @Override
    public void setLocalFilePath(Path pLocalFilePath) {
        localTarFile = pLocalFilePath;
    }

}