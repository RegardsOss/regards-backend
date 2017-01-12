package fr.cnes.regards.modules.storage.domain;

public class Semantic {

    private String description;

    public Semantic() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        description = pDescription;
    }

    public Semantic generate() {
        this.description = "DESCRIPTION";
        return this;
    }

}
