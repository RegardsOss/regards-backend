/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

import org.springframework.hateoas.Identifiable;

import fr.cnes.regards.domain.annotation.InstanceEntity;

@ValidateOnExecution
@InstanceEntity
@Entity(name = "T_PROJECT")
@SequenceGenerator(name = "projectSequence", initialValue = 1, sequenceName = "SEQ_PROJECT")
public class Project implements Identifiable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "projectSequence")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "name")
    private String name;

    @NotNull
    @Column(name = "description")
    private String description;

    @Column(name = "icon")
    private String icon;

    @NotNull
    @Column(name = "public")
    private boolean isPublic;

    @Column(name = "deleted")
    private boolean isDeleted;

    public Project() {
    }

    public Project(final Long pId, final String pDesc, final String pIcon, final boolean pIsPublic,
            final String pName) {
        this();
        id = pId;
        description = pDesc;
        icon = pIcon;
        isPublic = pIsPublic;
        name = pName;
    }

    public Project(final String pDesc, final String pIcon, final boolean pIsPublic, final String pName) {
        this();
        description = pDesc;
        icon = pIcon;
        isPublic = pIsPublic;
        name = pName;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String pName) {
        name = pName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String pDescription) {
        description = pDescription;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(final String pIcon) {
        icon = pIcon;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(final boolean pIsPublic) {
        isPublic = pIsPublic;
    }

    @Override
    public boolean equals(final Object o) {
        return (o instanceof Project) && ((Project) o).getId().equals(id);
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(final boolean pIsDeleted) {
        isDeleted = pIsDeleted;
    }

}
