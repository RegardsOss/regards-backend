/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

public class ProvenanceInformation implements Serializable {

    @NotNull
    private String facility;

    @NotNull
    private List<Event> history;

    private Map<String, Object> additional;

    public ProvenanceInformation(String pFacility, List<Event> pHistory) {
        super();
        facility = pFacility;
        history = pHistory;
    }

    /**
     *
     */
    public ProvenanceInformation() {
        // TODO Auto-generated constructor stub
    }

    public String getFacility() {
        return facility;
    }

    public void setFacility(String pFacility) {
        facility = pFacility;
    }

    public List<Event> getHistory() {
        return history;
    }

    public void setHistory(List<Event> pHistory) {
        history = pHistory;
    }

    public void addHistoryElement(Event pElement) {
        history.add(pElement);
    }

    public Map<String, Object> getAdditional() {
        return additional;
    }

    public void setAdditional(Map<String, Object> pAdditional) {
        additional = pAdditional;
    }

    public ProvenanceInformation generate() {
        facility = "TestPerf";
        history = new ArrayList<>();
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((additional == null) ? 0 : additional.hashCode());
        result = (prime * result) + ((facility == null) ? 0 : facility.hashCode());
        result = (prime * result) + ((history == null) ? 0 : history.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ProvenanceInformation other = (ProvenanceInformation) obj;
        if (additional == null) {
            if (other.additional != null) {
                return false;
            }
        } else
            if (!additional.equals(other.additional)) {
                return false;
            }
        if (facility == null) {
            if (other.facility != null) {
                return false;
            }
        } else
            if (!facility.equals(other.facility)) {
                return false;
            }
        if (history == null) {
            if (other.history != null) {
                return false;
            }
        } else
            if (!history.containsAll(other.history)) {
                return false;
            }
        return true;
    }

}
