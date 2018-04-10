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
package fr.cnes.regards.modules.storage.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.manager.AbstractModuleConfigurationManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Configuration manager for current module
 * @author Marc Sordi
 */
@Component
public class StorageConfigurationManager extends AbstractModuleConfigurationManager {

    @Override
    public void importConfiguration(ModuleConfiguration configuration) throws ModuleException {
        // TODO
        // for (ModuleConfigurationItem<?> item : configuration.getConfiguration()) {
        // if (MyPOJO.class.isAssignableFrom(item.getKey())) {
        // MyPOJO pojo = item.getTypedValue();
        // // Do something to import configuration
        // }
        // }
    }

    @Override
    public ModuleConfiguration exportConfiguration() throws ModuleException {
        List<ModuleConfigurationItem<?>> configuration = new ArrayList<>();
        // Fill list using ModuleConfigurationItem#build
        // TODO
        return ModuleConfiguration.build(info, configuration);
    }
}
