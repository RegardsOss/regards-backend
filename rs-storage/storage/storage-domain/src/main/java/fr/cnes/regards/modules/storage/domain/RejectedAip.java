package fr.cnes.regards.modules.storage.domain;

import java.util.List;

/**
 * POJO allowing to known which aip has been rejected for an action and why
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class RejectedAip {

    /**
     * Aip ip id
     */
    private String aipId;

    /**
     * causes for rejection
     */
    private List<String> rejectionCauses;

    /**
     * Constructor setting the parameters as attributes
     * @param aipId
     * @param rejectionCause
     */
    public RejectedAip(String aipId, List<String> rejectionCause) {
        this.aipId = aipId;
        this.rejectionCauses = rejectionCause;
    }

    /**
     * @return the ip id
     */
    public String getAipId() {
        return aipId;
    }

    /**
     * Set the ip id
     * @param ipId
     */
    public void setAipId(String ipId) {
        this.aipId = ipId;
    }

    /**
     * @return the rejection causes
     */
    public List<String> getRejectionCauses() {
        return rejectionCauses;
    }

    /**
     * Set the rejection causes
     * @param rejectionCauses
     */
    public void setRejectionCauses(List<String> rejectionCauses) {
        this.rejectionCauses = rejectionCauses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RejectedAip that = (RejectedAip) o;

        return aipId != null ? aipId.equals(that.aipId) : that.aipId == null;
    }

    @Override
    public int hashCode() {
        return aipId != null ? aipId.hashCode() : 0;
    }
}
