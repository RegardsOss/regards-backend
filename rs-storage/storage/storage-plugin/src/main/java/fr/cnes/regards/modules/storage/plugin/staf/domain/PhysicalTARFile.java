package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.nio.file.Path;
import java.util.Set;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

public class PhysicalTARFile extends STAFPhysicalFile {

    private final String stafFileName;

    private final Set<Path> localFiles;

    private Path localTarFile;

    /**
     * Status of the current Tar file.<br/>
     * Does the TAR raised the conditions to be send to the STAF System ?<br/>
     * If not, the TAR stays in workspace waiting for new files to add in.
     */
    private boolean toStore;

    public PhysicalTARFile(String pSTAFNode, Set<Path> pLocalFiles, String pSTAFFileName, Path pLocalTarFile) {
        super(STAFArchiveModeEnum.TAR, pSTAFNode);
        localFiles = pLocalFiles;
        stafFileName = pSTAFFileName;
        localTarFile = pLocalTarFile;
        toStore = false;
    }

    public boolean isToStore() {
        return toStore;
    }

    public void setToStore(boolean pToStore) {
        toStore = pToStore;
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

}