package fr.cnes.regards.modules.project.domain;

import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

import org.springframework.hateoas.ResourceSupport;

@ValidateOnExecution
public class Project extends ResourceSupport {

    @NotNull
    private String name_;

    @NotNull
    private String description_;

    private String icon_;

    @NotNull
    private boolean isPublic_;

    private boolean isDeleted_;

    public Project() {
        super();
    }

    public Project(String desc, String icon, boolean isPublic, String name) {
        this();
        this.description_ = desc;
        this.icon_ = icon;
        this.isPublic_ = isPublic;
        this.name_ = name;
    }

    public String getDescription() {
        return description_;
    }

    public void setDescription(String pDescription) {
        description_ = pDescription;
    }

    public String getIcon() {
        return icon_;
    }

    public void setIcon(String pIcon) {
        icon_ = pIcon;
    }

    public boolean isPublic() {
        return isPublic_;
    }

    public void setPublic(boolean pIsPublic) {
        isPublic_ = pIsPublic;
    }

    public String getName() {
        return name_;
    }

    public void setName(String pName) {
        name_ = pName;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Project) {
            Project p = (Project) o;
            return p.name_.equals(this.name_) && this.description_.equals(p.description_) && this.icon_.equals(p.icon_)
                    && (this.isPublic_ == p.isPublic_);
        }

        return false;
    }

    public boolean isDeleted() {
        return isDeleted_;
    }

    public void setDeleted(boolean pIsDeleted) {
        isDeleted_ = pIsDeleted;
    }

}
