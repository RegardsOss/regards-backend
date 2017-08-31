package fr.cnes.regards.modules.storage.plugin.staf.domain;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;

public class PhysicalFile extends STAFPhysicalFile {

    private String stafLocation;

    private String localLocation;

    public PhysicalFile() {
        super(STAFArchiveModeEnum.NORMAL);
    }

    public String getStafLocation() {
        return stafLocation;
    }

    public void setStafLocation(String pStafLocation) {
        stafLocation = pStafLocation;
    }

    public String getLocalLocation() {
        return localLocation;
    }

    public void setLocalLocation(String pLocalLocation) {
        localLocation = pLocalLocation;
    }

}
