/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.domain.ui;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.access.services.domain.validation.NotEmptyFieldsIfService;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;

/**
 *
 * Class Plugin
 *
 * Entity to describe an IHM Plugin definition.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Entity
@Table(name = "t_ui_plugin")
@NotEmptyFieldsIfService
public class UIPluginDefinition {

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ihmPluginsSequence", initialValue = 1, sequenceName = "seq_ui_plugin")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihmPluginsSequence")
    private Long id;

    /**
     * Theme name. Use to instantiate the right module
     */
    @NotNull
    @Column(nullable = false, length = 32)
    private String name;

    /**
     * Theme name. Use to instantiate the right module
     */
    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UIPluginTypesEnum type;

    /**
     * Theme name. Use to instantiate the right module
     */
    @NotNull
    @Column(nullable = false)
    private String sourcePath;

    /**
     * Icon of the plugin. It must be an URL to a svg file.
     */
    @Column(name = "icon_url")
    private URL iconUrl;

    /**
     * Application modes
     */
    @NotNull
    @Column(name = "application_mode", nullable = false)
    @ElementCollection
    @CollectionTable(name = "t_ui_plugin_application_mode", joinColumns = @JoinColumn(name = "ui_plugin_id"),
            foreignKey = @ForeignKey(name = "fk_ui_plugin_application_mode_ui_plugin_id"))
    @Enumerated(EnumType.STRING)
    private Set<ServiceScope> applicationModes = new HashSet<>();

    /**
     * Entity Types to which this plugin is applicable
     */
    @NotNull
    @Column(name = "entity_type", nullable = false)
    @ElementCollection
    @CollectionTable(name = "t_ui_plugin_entity_type", joinColumns = @JoinColumn(name = "ui_plugin_id"),
            foreignKey = @ForeignKey(name = "fk_ui_plugin_entity_type_ui_plugin_id"))
    @Enumerated(EnumType.STRING)
    private Set<EntityType> entityTypes = new HashSet<>();

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

    public UIPluginTypesEnum getType() {
        return type;
    }

    public void setType(final UIPluginTypesEnum pType) {
        type = pType;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(final String pSourcePath) {
        sourcePath = pSourcePath;
    }

    /**
     * @return the iconUrl
     */
    public URL getIconUrl() {
        return iconUrl;
    }

    /**
     * @param pIconUrl the iconUrl to set
     */
    public void setIconUrl(URL pIconUrl) {
        iconUrl = pIconUrl;
    }

    /**
     * @return the applicationModes
     */
    public Set<ServiceScope> getApplicationModes() {
        return applicationModes;
    }

    /**
     * @param pApplicationModes the applicationModes to set
     */
    public void setApplicationModes(Set<ServiceScope> pApplicationModes) {
        applicationModes = pApplicationModes;
    }

    /**
     * @return the entityTypes
     */
    public Set<EntityType> getEntityTypes() {
        return entityTypes;
    }

    /**
     * @param pEntityTypes the entityTypes to set
     */
    public void setEntityTypes(Set<EntityType> pEntityTypes) {
        entityTypes = pEntityTypes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UIPluginDefinition other = (UIPluginDefinition) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

}
