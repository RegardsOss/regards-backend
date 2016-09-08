package fr.cnes.regards.modules.project.domain;

import org.springframework.hateoas.ResourceSupport;

public class Project extends ResourceSupport {

    private String name;

    private String description;

    private String icon;

    private boolean isPublic;

    public Project() {
        super();
    }

    public Project(String desc, String icon, boolean isPublic, String name) {
        this();
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

    @Override
    public boolean equals(Object o) {
        if (o instanceof Project) {
            Project p = (Project) o;
            return p.name.equals(this.name) && this.description.equals(p.description) && this.icon.equals(p.icon)
                    && (this.isPublic == p.isPublic);
        }

        return false;
    }

}
