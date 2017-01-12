package fr.cnes.regards.modules.storage.domain;

import java.util.ArrayList;
import java.util.List;

public class Information {

    private List<KeyValuePair> metadata;

    public Information() {
        this.metadata = new ArrayList<>();
    }

    public List<KeyValuePair> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<KeyValuePair> pMetadata) {
        metadata = pMetadata;
    }

    public void addMetadata(KeyValuePair metadata) {
        this.metadata.add(metadata);
    }

}
