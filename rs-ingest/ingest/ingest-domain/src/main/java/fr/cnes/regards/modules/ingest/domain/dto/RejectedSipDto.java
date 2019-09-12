package fr.cnes.regards.modules.ingest.domain.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO allowing to known why a sip has been rejected
 * @author Sylvain VISSIERE-GUERINET
 *
 * FIXME revoir la suppression des SIP/AIP
 */
public class RejectedSipDto {

    /**
     * Sip id
     */
    private String sipId;

    /**
     * Causes of rejection
     */
    private List<String> rejectionCauses;

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

        RejectedSipDto that = (RejectedSipDto) o;

        return sipId != null ? sipId.equals(that.sipId) : that.sipId == null;
    }

    @Override
    public int hashCode() {
        return sipId != null ? sipId.hashCode() : 0;
    }

    public static RejectedSipDto build(String sipId, String rejectionCause) {
        RejectedSipDto rejectedAip = new RejectedSipDto();
        rejectedAip.setSipId(sipId);
        List<String> rejectionCauses = new ArrayList<>();
        rejectionCauses.add(rejectionCause);
        rejectedAip.setRejectionCauses(rejectionCauses);
        return rejectedAip;
    }
}
