package fr.cnes.regards.modules.users.domain;

public class MetaData {

    private String key_;

    private String value_;

    private UserVisibility visibility_;

    public String getKey() {
        return key_;
    }

    public void setKey(String pKey) {
        key_ = pKey;
    }

    public String getValue() {
        return value_;
    }

    public void setValue(String pValue) {
        value_ = pValue;
    }

    public UserVisibility getVisibility() {
        return visibility_;
    }

    public void setVisibility(UserVisibility pVisibility) {
        visibility_ = pVisibility;
    }

}
