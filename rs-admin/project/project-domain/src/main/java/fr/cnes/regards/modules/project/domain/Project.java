/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.domain;

import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

import org.springframework.hateoas.Identifiable;

@ValidateOnExecution
public class Project implements Identifiable<Long> {

    private Long id_;

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

    public Project(Long pId, String pDesc, String pIcon, boolean pIsPublic, String pName) {
        this();
        id_ = pId;
        description_ = pDesc;
        icon_ = pIcon;
        isPublic_ = pIsPublic;
        name_ = pName;
    }

    @Override
    public Long getId() {
        return id_;
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
