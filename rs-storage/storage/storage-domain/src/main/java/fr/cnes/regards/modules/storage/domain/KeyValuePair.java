package fr.cnes.regards.modules.storage.domain;

public class KeyValuePair {

    private String key;

    private Object value;

    public KeyValuePair() {
    }

    public KeyValuePair(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String pKey) {
        key = pKey;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object pValue) {
        value = pValue;
    }
}
