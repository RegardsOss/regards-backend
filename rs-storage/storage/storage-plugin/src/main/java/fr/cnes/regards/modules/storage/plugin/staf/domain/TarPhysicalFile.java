package fr.cnes.regards.modules.storage.plugin.staf.domain;

import java.util.Set;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

public class TarPhysicalFile extends STAFPhysicalFile {

    private String tarLocalFileLocation;

    private Set<String> filesInTar;

    public TarPhysicalFile() {
        super(STAFArchiveModeEnum.TAR);
    }

    public String getTarLocalFileLocation() {
        return tarLocalFileLocation;
    }

    public void setTarLocalFileLocation(String pTarLocalFileLocation) {
        tarLocalFileLocation = pTarLocalFileLocation;
    }

    public Set<String> getFilesInTar() {
        return filesInTar;
    }

    public void setFilesInTar(Set<String> pFilesInTar) {
        filesInTar = pFilesInTar;
    }

}