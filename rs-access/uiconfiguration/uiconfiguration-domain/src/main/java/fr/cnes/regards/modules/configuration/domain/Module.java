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
package fr.cnes.regards.modules.configuration.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Class Module
 * <p>
 * Entity to describe an IHM module configuration.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Entity
@Table(name = "t_ui_module")
public class Module {

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ihmModulesSequence", initialValue = 1, sequenceName = "seq_ui_module")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihmModulesSequence")
    private Long id;

    /**
     * Module type. Use to instantiate the right module
     */
    @NotNull
    @Column(nullable = false, length = 32)
    private String type;

    /**
     * Module description label.
     */
    @NotNull
    @Column(nullable = false, length = 64)
    private String description;

    /**
     * The application where the module must be displayed
     */
    @NotNull
    @Column(nullable = false, length = 16)
    private String applicationId;

    /**
     * The container of the application layout where the module must be displayed
     */
    @NotNull
    @Column(nullable = false)
    private String container;

    /**
     * Module configuration (JSON Object)
     */
    @Column(nullable = false, columnDefinition = "text")
    private String conf;

    /**
     * Does the module is active ?
     */
    @NotNull
    @Column(nullable = false)
    private boolean active;

    @Embedded
    private UIPage page;

    public Long getId() {
        return id;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    public String getType() {
        return type;
    }

    public void setType(final String pType) {
        type = pType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String pDescription) {
        description = pDescription;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(final String pApplicationId) {
        applicationId = pApplicationId;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(final String pContainer) {
        container = pContainer;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(final String pConf) {
        conf = pConf;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean pActive) {
        active = pActive;
    }

    public UIPage getPage() {
        return page;
    }

    public void setPage(UIPage page) {
        this.page = page;
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
        final Module other = (Module) obj;
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
        return "Module [id="
               + id
               + ", type="
               + type
               + ", description="
               + description
               + ", applicationId="
               + applicationId
               + ", container="
               + container
               + ", conf="
               + conf
               + ", active="
               + active
               + "]";
    }

}
