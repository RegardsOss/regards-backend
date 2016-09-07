package fr.cnes.regards.modules.project.domain;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Project extends ResourceSupport {

    private final long id = 1L;

    @JsonProperty
    private String description;

    @JsonProperty
    private String icon;

    @JsonProperty
    private boolean isPublic;

    @JsonProperty
    private String name;

    public Project() {
        super();
    }

    public Project(String desc, String icon, boolean isPublic, String name) {
        this.description = desc;
        this.icon = icon;
        this.isPublic = isPublic;
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        description = pDescription;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String pIcon) {
        icon = pIcon;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean pIsPublic) {
        isPublic = pIsPublic;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

}
