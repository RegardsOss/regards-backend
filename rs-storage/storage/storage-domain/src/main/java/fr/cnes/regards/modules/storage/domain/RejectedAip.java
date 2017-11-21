package fr.cnes.regards.modules.storage.domain;

import java.util.List;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class RejectedAip {

    private String ipId;

    private List<String> rejectionCauses;

    public RejectedAip(String ipId, List<String> rejectionCause) {
        this.ipId = ipId;
        this.rejectionCauses = rejectionCause;
    }

    public String getIpId() {
        return ipId;
    }

    public void setIpId(String ipId) {
        this.ipId = ipId;
    }

    public List<String> getRejectionCauses() {
        return rejectionCauses;
    }

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
