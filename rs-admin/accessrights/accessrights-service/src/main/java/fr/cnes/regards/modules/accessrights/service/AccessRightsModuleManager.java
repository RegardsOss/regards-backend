/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Configuration manager for current module
 *
 * @author Marc Sordi
 */
@Service
public class AccessRightsModuleManager extends AbstractModuleManager<Void> {

    @Override
    public ModuleConfiguration exportConfiguration() {
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();
        // Fill list using ModuleConfigurationItem#build
        return ModuleConfiguration.build(info, configurations);
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration) {
        return Sets.newHashSet();
    }

}
