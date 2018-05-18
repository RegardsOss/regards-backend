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
    private String ipId;

    /**
     * causes for rejection
     */
    private List<String> rejectionCauses;

    /**
     * Constructor setting the parameters as attributes
     * @param ipId
     * @param rejectionCause
     */
    public RejectedAip(String ipId, List<String> rejectionCause) {
        this.ipId = ipId;
        this.rejectionCauses = rejectionCause;
    }

    /**
     * @return the ip id
     */
    public String getIpId() {
        return ipId;
    }

    /**
     * Set the ip id
     * @param ipId
     */
    public void setIpId(String ipId) {
        this.ipId = ipId;
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

        return ipId != null ? ipId.equals(that.ipId) : that.ipId == null;
    }

    @Override
    public int hashCode() {
        return ipId != null ? ipId.hashCode() : 0;
    }
}
