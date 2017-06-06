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
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.executable.ValidateOnExecution;

import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;

/**
 * Class Project Project Entity
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@ValidateOnExecution
@InstanceEntity
@Entity
@Table(name = "t_project", uniqueConstraints = @UniqueConstraint(name = "uk_project_name", columnNames = { "name" }))
@SequenceGenerator(name = "projectSequence", initialValue = 1, sequenceName = "seq_project")
public class Project implements IIdentifiable<Long> {

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
    @Pattern(regexp = "[a-zA-Z0-9-_]*", message = "Valid caracters for project name are 'a-z','A-Z','0-9','-' and '_'")
    @Column(name = "name", length = 30)
    private String name;

    /**
     * Project Label
     */
    @NotBlank
    @Column(length = 256)
    private String label;

    /**
     * Project description
     */
    @NotNull
    @Type(type = "text")
    @Column
    private String description;

    /**
     * Project image icon
     */
    @Column(name = "icon", length = 255)
    private String icon;

    /**
     * IS the project public ?
     */
    @Column(name = "public")
    @NotNull
    private Boolean isPublic;

    /**
     * IS the project accessible from portal ?
     */
    @Column(name = "accessible")
    @NotNull
    private Boolean isAccessible = false;

    /**
     * Is the project deleted ?
     */
    @Column(name = "deleted")
    @NotNull
    private Boolean isDeleted;

    /**
     * URL to the project's licence
     */
    @Column
    @Type(type = "text")
    private String licenceLink;

    /**
     * The project hostname(hostname+port is needed)
     */
    @Column
    private String host;

    public Project() {
        super();
        name = "undefined";
        description = "";
        isDeleted = false;
        isPublic = false;
        isAccessible = false;
    }

    public Project(final Long pId, final String pDesc, final String pIcon, final boolean pIsPublic,
            final String pName) {
        this();
        id = pId;
        description = pDesc;
        icon = pIcon;
        isPublic = pIsPublic;
        name = pName;
        isDeleted = false;
    }

    public Project(final String pDesc, final String pIcon, final boolean pIsPublic, final String pName) {
        this();
        description = pDesc;
        icon = pIcon;
        isPublic = pIsPublic;
        name = pName;
        isDeleted = false;
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

    public String getLicenceLink() {
        return licenceLink;
    }

    public void setLicenseLink(final String pLicenceLink) {
        licenceLink = pLicenceLink;
    }

    @Override
    public boolean equals(final Object pObject) {
        if (pObject == null) {
            return false;
        }

        if (this.getClass() != pObject.getClass()) {
            return false;
        }

        final Project projectToCompare = (Project) pObject;
        if ((id != null) && (projectToCompare.getId() != null)) {
            return (pObject instanceof Project) && (id.equals(((Project) pObject).getId()));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        } else {
            return 0;
        }
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(final boolean pIsDeleted) {
        isDeleted = pIsDeleted;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String pHost) {
        host = pHost;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getAccessible() {
        return isAccessible;
    }

    public void setAccessible(Boolean accessible) {
        isAccessible = accessible;
    }
}
