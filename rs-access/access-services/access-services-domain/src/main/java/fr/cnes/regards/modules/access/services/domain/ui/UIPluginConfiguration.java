/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Class PluginConfiguration
 * <p>
 * Entity to describe an IHM Plugin configuration.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Entity
@Table(name = "t_ui_plugin_configuration")
public class UIPluginConfiguration {

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ihmPluginConfsSequence", initialValue = 1, sequenceName = "seq_ihm_plugin_configuration")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihmPluginConfsSequence")
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "ui_plugin_id", foreignKey = @ForeignKey(name = "fk_ui_plugin_configuration_ui_plugin_id"))
    private UIPluginDefinition pluginDefinition;

    @NotNull
    @Column(nullable = false)
    private boolean active = false;

    @NotNull
    @Column(nullable = false, length = 32)
    private String label;

    /**
     * Parameter to define if by default this plugin configuration is linked to all entities.
     */
    @NotNull
    @Column(nullable = false)
    private boolean linkedToAllEntities = false;

    /**
     * Plugin configuration (JSON Object)
     */
    @Column(nullable = false, columnDefinition = "text")
    private String conf;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public UIPluginDefinition getPluginDefinition() {
        return pluginDefinition;
    }

    public void setPluginDefinition(final UIPluginDefinition pluginDefinition) {
        this.pluginDefinition = pluginDefinition;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(final Boolean active) {
        this.active = active;
    }

    public Boolean getLinkedToAllEntities() {
        return linkedToAllEntities;
    }

    public void setLinkedToAllEntities(final Boolean linkedToAllEntities) {
        this.linkedToAllEntities = linkedToAllEntities;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(final String conf) {
        this.conf = conf;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
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
        final UIPluginConfiguration other = (UIPluginConfiguration) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UIPluginConfiguration [id="
               + id
               + ", pluginDefinition="
               + pluginDefinition
               + ", active="
               + active
               + ", label="
               + label
               + ", linkedToAllEntities="
               + linkedToAllEntities
               + ", conf="
               + conf
               + "]";
    }

}
