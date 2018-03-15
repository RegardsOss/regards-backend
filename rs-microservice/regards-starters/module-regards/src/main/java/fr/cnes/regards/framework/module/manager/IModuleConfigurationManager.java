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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Bean contract for module configuration management
 *
 * @author Marc Sordi
 *
 */
public interface IModuleConfigurationManager {

    static final String PROPERTY_FILE = "module.properties";

    /**
     * Module information
     */
    ModuleInformation getModuleInformation();

    /**
     * Check if this configuration is applicable to current manager. If so,
     * {@link #importConfiguration(ModuleConfiguration)} will be called.
     */
    boolean isApplicable(ModuleConfiguration configuration);

    /**
     * Import configuration
     */
    void importConfiguration(ModuleConfiguration configuration) throws ModuleException;

    /**
     * Export configuration
     */
    ModuleConfiguration exportConfiguration() throws ModuleException;
}
