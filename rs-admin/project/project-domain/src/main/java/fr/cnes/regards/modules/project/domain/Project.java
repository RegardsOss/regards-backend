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

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;

/**
 *
 * Class Project
 *
 * Project Entity
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@ValidateOnExecution
@InstanceEntity
@Entity(name = "T_PROJECT")
@SequenceGenerator(name = "projectSequence", initialValue = 1, sequenceName = "SEQ_PROJECT")
public class Project implements Identifiable<Long> {

    /**
     * Project Unique Identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "projectSequence")
    @Column(name = "id")
    private Long id;

    /**
     * Project name
     */
    @NotNull
    @Column(name = "name", unique = true)
    private String name;

    /**
     * Project description
     */
    @NotNull
    @Column(name = "description")
    private String description;

    /**
     * Project image icon
     */
    @Column(name = "icon")
    private String icon;

    /**
     * IS the project public ?
     */
    @NotNull
    @Column(name = "public")
    private boolean isPublic;

    /**
     * Is the project deleted ?
     */
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
    public boolean equals(final Object pObject) {
        return (pObject instanceof Project) && ((Project) pObject).getId().equals(id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(final boolean pIsDeleted) {
        isDeleted = pIsDeleted;
    }

}
