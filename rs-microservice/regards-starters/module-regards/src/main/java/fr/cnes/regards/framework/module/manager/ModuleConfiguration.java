/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

/**
 * Module configuration wrapper
 *
 * @author Marc Sordi
 *
 */
public class ModuleConfiguration {

    private ModuleInformation module;

    private List<ModuleConfigurationItem<?>> configuration;

    public List<ModuleConfigurationItem<?>> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(List<ModuleConfigurationItem<?>> configuration) {
        this.configuration = configuration;
    }

    public ModuleInformation getModule() {
        return module;
    }

    public void setModule(ModuleInformation module) {
        this.module = module;
    }

    public static ModuleConfiguration build(ModuleInformation info, List<ModuleConfigurationItem<?>> configuration) {
        ModuleConfiguration moduleConfiguration = new ModuleConfiguration();
        moduleConfiguration.setModule(info);
        moduleConfiguration.setConfiguration(configuration);
        return moduleConfiguration;
    }
}
