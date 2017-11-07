package fr.cnes.regards.modules.storage.domain;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class RejectedSip {

    private String sipId;

    private ModuleException e;

    public RejectedSip(String sipIpId, ModuleException e) {
        this.sipId = sipIpId;
        this.e = e;
    }

    public String getSipId() {
        return sipId;
    }

    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    public ModuleException getE() {
        return e;
    }

    public void setE(ModuleException e) {
        this.e = e;
    }
}
