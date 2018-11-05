package fr.cnes.regards.modules.storage.domain;

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
     */
    public RejectedSip(String sipId, String reason) {
        this.sipId = sipId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        RejectedSip that = (RejectedSip) o;

        return sipId != null ? sipId.equals(that.sipId) : that.sipId == null;
    }

    @Override
    public int hashCode() {
        return sipId != null ? sipId.hashCode() : 0;
    }
}
