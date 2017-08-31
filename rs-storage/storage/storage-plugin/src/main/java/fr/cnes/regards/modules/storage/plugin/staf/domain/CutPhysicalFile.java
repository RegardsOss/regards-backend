package fr.cnes.regards.modules.storage.plugin.staf.domain;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

public class CutPhysicalFile extends STAFPhysicalFile {

    private String localUncutedFileLocation;

    private String stafCutFileLocation;

    public CutPhysicalFile(String pLocalUncutedPhysicalFile, String pStafCutFileLocation) {
        super(STAFArchiveModeEnum.CUT);
        localUncutedFileLocation = pLocalUncutedPhysicalFile;
        stafCutFileLocation = pStafCutFileLocation;
    }

    public String getLocalUncutedFileLocation() {
        return localUncutedFileLocation;
    }

    public void setLocalUncutedFileLocation(String pLocalUncutedFileLocation) {
        localUncutedFileLocation = pLocalUncutedFileLocation;
    }

    public String getStafCutFileLocation() {
        return stafCutFileLocation;
    }

    public void setStafCutFileLocation(String pStafCutFileLocation) {
        stafCutFileLocation = pStafCutFileLocation;
    }

}
