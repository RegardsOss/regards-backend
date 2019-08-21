package fr.cnes.regards.modules.ingest.domain.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO allowing to known which aip has been rejected for an action and why
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class RejectedAipDto {

    /**
     * Aip ip id
     */
    private String aipId;

    /**
     * causes for rejection
     */
    private List<String> rejectionCauses;

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

        RejectedAipDto that = (RejectedAipDto) o;

        return aipId != null ? aipId.equals(that.aipId) : that.aipId == null;
    }

    @Override
    public int hashCode() {
        return aipId != null ? aipId.hashCode() : 0;
    }

    public static RejectedAipDto build(String aipId, String rejectionCause) {
        RejectedAipDto rejectedAip = new RejectedAipDto();
        rejectedAip.setAipId(aipId);
        List<String> rejectionCauses = new ArrayList<>();
        rejectionCauses.add(rejectionCause);
        rejectedAip.setRejectionCauses(rejectionCauses);
        return rejectedAip;
    }
}
