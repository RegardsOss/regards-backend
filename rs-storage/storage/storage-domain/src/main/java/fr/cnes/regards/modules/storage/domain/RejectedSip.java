package fr.cnes.regards.modules.storage.domain;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * POJO allowing to known why a sip has been rejected
 * @author Sylvain VISSIERE-GUERINET
 */
public class RejectedSip {

    /**
     * Sip id
     */
    private String sipId;

    /**
     * Exception causing the rejection
     */
    private String reason;

    /**
     * Constructor setting the parameters as attributes
     * @param sipIpId
     * @param reason
     */
    public RejectedSip(String sipIpId, String reason) {
        this.sipId = sipIpId;
        this.reason = reason;
    }

    /**
     * @return the sip id
     */
    public String getSipId() {
        return sipId;
    }

    /**
     * Set the sip id
     * @param sipId
     */
    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    /**
     * @return the reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Set the reason
     * @param reason
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
}
