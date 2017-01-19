/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.util.ArrayList;
import java.util.List;

public class ProvenanceInformation extends Information {

    private transient String facility;

    private transient List<Event> history;

    public ProvenanceInformation() {
        super();
    }

    public String getFacility() {
        return facility;
    }

    public void setFacility(String pFacility) {
        facility = pFacility;
        addMetadata("facility", facility);
    }

    public List<Event> getHistory() {
        return history;
    }

    public void setHistory(List<Event> pHistory) {
        history = pHistory;
        addMetadata("history", history);
    }

    public void addHistoryElement(Event element) {
        history.add(element);
        addMetadata("history", history);
    }

    public ProvenanceInformation generate() {
        facility = "TestPerf";
        addMetadata("facility", facility);
        history = new ArrayList<>();
        addMetadata("history", history);
        return this;
    }

}
