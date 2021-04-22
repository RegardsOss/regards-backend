/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.executable.ValidateOnExecution;

import org.hibernate.annotations.Type;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;

/**
 * Class Project Project Entity
 * @author Sébastien Binda
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
    @Pattern(regexp = "[a-zA-Z0-9-_]*", message = "Valid characters for project name are 'a-z','A-Z','0-9','-' and '_'")
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
     * Is the project accessible from portal ?
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

    @Column(name = "crs", length = 32)
    private String crs = "WGS_84";

    @Column(name = "pole_managed")
    private Boolean isPoleToBeManaged = false;

    /**
     * Default constructor
     */
    public Project() {
        name = "undefined";
        description = "";
        isDeleted = false;
        isPublic = false;
        isAccessible = false;
    }

    /**
     * @deprecated don't use this method, giving id is totally nonsense (and is not taken into account)
     */
    @Deprecated
    public Project(Long id, String desc, String icon, boolean isPublic, String name) {
        this.id = id;
        description = desc;
        this.icon = icon;
        this.isPublic = isPublic;
        this.name = name;
        isDeleted = false;
    }

    public Project(String desc, String icon, boolean isPublic, String name) {
        description = desc;
        this.icon = icon;
        this.isPublic = isPublic;
        this.name = name;
        isDeleted = false;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String desc) {
        description = desc;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getLicenceLink() {
        return licenceLink;
    }

    public void setLicenseLink(String licenceLink) {
        this.licenceLink = licenceLink;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    public Boolean getPoleToBeManaged() {
        return isPoleToBeManaged;
    }

    public void setPoleToBeManaged(Boolean poleToBeManaged) {
        isPoleToBeManaged = poleToBeManaged;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }

        Project projectToCompare = (Project) obj;
        if ((id != null) && (projectToCompare.getId() != null)) {
            return (obj instanceof Project) && (id.equals(((Project) obj).getId()));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        if (id == null) {
            return 0;
        } else {
            return id.hashCode();
        }
    }

    /**
     * @return whether the project is deleted or not
     */
    public boolean isDeleted() {
        return isDeleted;
    }

    /**
     * Set whether the project is deleted or not
     */
    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return whether the project is accessible or not
     */
    public Boolean getAccessible() {
        return isAccessible;
    }

    /**
     * Set whether the project is accessible or not
     */
    public void setAccessible(Boolean accessible) {
        isAccessible = accessible;
    }
}
