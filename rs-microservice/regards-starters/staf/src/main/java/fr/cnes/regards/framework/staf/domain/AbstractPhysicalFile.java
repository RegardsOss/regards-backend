/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.domain;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.staf.TARController;
import fr.cnes.regards.framework.staf.exception.STAFException;

/**
 * Abstract class to define standard parameters of a STAF File.
 *
 * @author Sébastien Binda
 */
public abstract class AbstractPhysicalFile {

    /**
     * Class logger
     */
    protected static final Logger LOG = LoggerFactory.getLogger(TARController.class);

    /**
     * STAF Archiving mode TAR|CUT|NORMAL {@link STAFArchiveModeEnum}
     */
    private final STAFArchiveModeEnum archiveMode;

    /**
     * STAF Node is the path into the STAF Archive where to store the current file.
     */
    private final Path stafNode;

    /**
     * Name of the STAF Archive where the file is stored.
     */
    private final String stafArchiveName;

    /**
     * STAF File name.
     */
    private final String stafFileName;

    /**
     * file status
     */
    protected PhysicalFileStatusEnum status;

    /**
     * {@link Set} of {@link Path} of raw file(s) to send to STAF before transformation (TAR or CUT).
     */
    private Set<Path> rawlAssociatedFiles = Sets.newHashSet();

    /**
     *
     * @param pArchiveMode {@link STAFArchiveModeEnum} STAF Archiving mode TAR|CUT|NORMAL
     * @param pSTAFArchiveName {@link String} Name of the STAF Archive where the file is stored.
     * @param pSTAFNode {@link String} Path into the STAF Archive where to store the current file.
     * @param pIsReadyForSTAFTransfer {@link Boolean} Does this file is ready to be transfer to STAF ?
     */
    public AbstractPhysicalFile(STAFArchiveModeEnum pArchiveMode, String pSTAFArchiveName, Path pSTAFNode,
            String pSTAFFileName, PhysicalFileStatusEnum pStatus) {
        super();
        archiveMode = pArchiveMode;
        stafArchiveName = pSTAFArchiveName;
        stafNode = pSTAFNode;
        stafFileName = pSTAFFileName;
        status = pStatus;
    }

    /**
     * Return the local file to transfer to STAF when ready.
     * @return {@link Path} Local file path
     */
    public abstract Path getLocalFilePath();

    /**
     * Allow to update the local file path for restored files.
     */
    public abstract void setLocalFilePath(Path pLocalFilePath);

    /**
     * Return the STAF Path where to transfer file when ready.
     * @return {@link Path} STAF File path
     * @throws STAFException Error during STAF Path creation.
     */
    public Path getSTAFFilePath() {
        return Paths.get(stafNode.toString(), stafFileName);
    }

    public STAFArchiveModeEnum getArchiveMode() {
        return archiveMode;
    }

    public Path getStafNode() {
        return stafNode;
    }

    public Set<Path> getRawAssociatedFiles() {
        return rawlAssociatedFiles;
    }

    public void setRawAssociatedFiles(Set<Path> pRawAssociatedFiles) {
        rawlAssociatedFiles = pRawAssociatedFiles;
    }

    public void addRawAssociatedFile(Path pRawAssociatedFile) {
        rawlAssociatedFiles.add(pRawAssociatedFile);
    }

    public PhysicalFileStatusEnum getStatus() {
        return status;
    }

    public void setStatus(PhysicalFileStatusEnum pStatus) {
        status = pStatus;
    }

    public String getStafArchiveName() {
        return stafArchiveName;
    }

    public String getStafFileName() {
        return stafFileName;
    }

}
