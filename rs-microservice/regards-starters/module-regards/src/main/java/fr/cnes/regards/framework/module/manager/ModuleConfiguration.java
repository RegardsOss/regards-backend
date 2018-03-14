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
package fr.cnes.regards.framework.module.manager;

import java.util.List;

import org.hibernate.validator.constraints.NotBlank;

/**
 * Module information are loaded from <b>module.properties</b> that must exist in same package as {@link IModuleManager}
 * bean implementation.
 *
 * @author Marc Sordi
 *
 */
public class ModuleConfiguration {

    /**
     * @return name of the module
     */
    @NotBlank
    private String name;

    /**
     *
     * @return description of the module
     */
    private String description;

    /**
     *
     * @return version of the module
     */
    @NotBlank
    private String version;

    /**
     *
     * @return author of the module
     */
    @NotBlank
    private String author;

    /**
     *
     * @return legal owner of the module
     */
    private String legalOwner;

    /**
     *
     * @return link to the documentation of the module
     */
    private String documentation;

    private List<ModuleConfigurationItem> configuration;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLegalOwner() {
        return legalOwner;
    }

    public void setLegalOwner(String legalOwner) {
        this.legalOwner = legalOwner;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public List<ModuleConfigurationItem> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(List<ModuleConfigurationItem> configuration) {
        this.configuration = configuration;
    }
}
