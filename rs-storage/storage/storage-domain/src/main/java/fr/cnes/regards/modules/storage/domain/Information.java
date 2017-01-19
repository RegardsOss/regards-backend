/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.util.HashMap;
import java.util.Map;

public class Information {

    private Map<String, Object> metadata;

    public Information() {
        metadata = new HashMap<>();
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> pMetadata) {
        metadata = pMetadata;
    }

    public void addMetadata(String pKey, Object pValue) {
        metadata.put(pKey, pValue);
    }

}
