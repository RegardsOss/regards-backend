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
package fr.cnes.regards.modules.access.services.domain.ui;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import fr.cnes.regards.framework.urn.EntityType;
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
    private String iconUrl;

    /**
     * Application modes
     *
     * FetchType.EAGER : It is only an enumeration of values. No need to define a entity graph for this.
     */
    @NotNull
    @Column(name = "application_mode", nullable = false)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "t_ui_plugin_application_mode", joinColumns = @JoinColumn(name = "ui_plugin_id"),
            foreignKey = @ForeignKey(name = "fk_ui_plugin_application_mode_ui_plugin_id"))
    @Enumerated(EnumType.STRING)
    private Set<ServiceScope> applicationModes = new HashSet<>();

    /**
     * Entity Types to which this plugin is applicable
     *
     * FetchType.EAGER : It is only an enumeration of values. No need to define a entity graph for this.
     */
    @NotNull
    @Column(name = "entity_type", nullable = false)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "t_ui_plugin_entity_type", joinColumns = @JoinColumn(name = "ui_plugin_id"),
            foreignKey = @ForeignKey(name = "fk_ui_plugin_entity_type_ui_plugin_id"))
    @Enumerated(EnumType.STRING)
    private Set<EntityType> entityTypes = new HashSet<>();

    /**
     * Minimal role to return the module on user app
     */
    @Column(name = "role_name", nullable = false)
    private String roleName;

    public static UIPluginDefinition build(String name, String sourcePath, UIPluginTypesEnum type) {
        return UIPluginDefinition.build(name, sourcePath, type, "PUBLIC");
    }

    public static UIPluginDefinition build(String name, String sourcePath, UIPluginTypesEnum type, String roleName) {
        UIPluginDefinition pluginDefinition = new UIPluginDefinition();
        pluginDefinition.setName(name);
        pluginDefinition.setSourcePath(sourcePath);
        pluginDefinition.setType(type);
        pluginDefinition.setRoleName(roleName);
        return pluginDefinition;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public UIPluginTypesEnum getType() {
        return type;
    }

    public void setType(final UIPluginTypesEnum type) {
        this.type = type;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(final String sourcePath) {
        this.sourcePath = sourcePath;
    }

    /**
     * @return the iconUrl
     */
    public String getIconUrl() {
        return iconUrl;
    }

    /**
     * @param iconUrl the iconUrl to set
     */
    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    /**
     * @return the applicationModes
     */
    public Set<ServiceScope> getApplicationModes() {
        return applicationModes;
    }

    /**
     * @param applicationModes the applicationModes to set
     */
    public void setApplicationModes(Set<ServiceScope> applicationModes) {
        this.applicationModes = applicationModes;
    }

    /**
     * @return the entityTypes
     */
    public Set<EntityType> getEntityTypes() {
        return entityTypes;
    }

    /**
     * @param entityTypes the entityTypes to set
     */
    public void setEntityTypes(Set<EntityType> entityTypes) {
        this.entityTypes = entityTypes;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
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
