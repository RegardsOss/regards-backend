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
    private ModuleException exception;

    /**
     * Constructor setting the parameters as attributes
     * @param sipIpId
     * @param exception
     */
    public RejectedSip(String sipIpId, ModuleException exception) {
        this.sipId = sipIpId;
        this.exception = exception;
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
     * @return the exception
     */
    public ModuleException getException() {
        return exception;
    }

    /**
     * Set the exception
     * @param exception
     */
    public void setException(ModuleException exception) {
        this.exception = exception;
    }
}
