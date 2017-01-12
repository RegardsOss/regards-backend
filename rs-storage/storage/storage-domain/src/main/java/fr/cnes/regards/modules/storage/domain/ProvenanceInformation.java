package fr.cnes.regards.modules.storage.domain;

import java.util.ArrayList;
import java.util.List;

public class ProvenanceInformation extends Information {

    private String facility;

    private List<Event> history;

    public ProvenanceInformation() {
        super();
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

    public void addHistoryElement(Event element) {
        this.history.add(element);
    }

    public ProvenanceInformation generate() {
        this.facility = "TestPerf";
        this.addMetadata(new KeyValuePair("facility", this.facility));
        this.history = new ArrayList<>();
        this.addMetadata(new KeyValuePair("history", this.history));
        return this;
    }

}
