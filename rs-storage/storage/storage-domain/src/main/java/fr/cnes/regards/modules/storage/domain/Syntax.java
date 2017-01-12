package fr.cnes.regards.modules.storage.domain;

public class Syntax {

    private String description;

    private String mimeType;

    private String name;

    public Syntax() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        description = pDescription;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String pMimeType) {
        mimeType = pMimeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public Syntax generate() {
        this.description = "SYNTAX_DESCRIPTION";
        this.mimeType = "MIME_TYPE";
        this.name = "NAME";
        return this;
    }

}
