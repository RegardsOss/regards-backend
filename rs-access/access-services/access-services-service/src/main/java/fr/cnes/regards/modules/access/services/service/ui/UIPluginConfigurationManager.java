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
package fr.cnes.regards.modules.access.services.service.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.manager.ModuleReadinessReport;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;

/**
 *
 * Component to import/export module configuration
 *
 * @author Sébastien Binda
 *
 */
@Component
public class UIPluginConfigurationManager extends AbstractModuleManager<Void> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UIPluginConfigurationManager.class);

    @Autowired
    private IUIPluginDefinitionService pluginDefService;

    @Autowired
    private IUIPluginConfigurationService pluginConfService;

    @Override
    public ModuleConfiguration exportConfiguration() throws ModuleException {
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();
        for (UIPluginDefinition pluginDef : pluginDefService.retrievePlugins(PageRequest.of(0, 1_000))) {
            pluginDef.setId(null);
            configurations.add(ModuleConfigurationItem.build(pluginDef));
        }
        for (UIPluginConfiguration conf : pluginConfService.retrievePluginConfigurations(PageRequest.of(0, 1_000))) {
            configurations.add(ModuleConfigurationItem.build(UIPluginDefConfAssociation.build(conf)));
        }
        return ModuleConfiguration.build(info, configurations);
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration moduleConfiguration) {
        Set<String> importErrors = new HashSet<>();
        for (ModuleConfigurationItem<?> item : moduleConfiguration.getConfiguration()) {
            try {
                if (UIPluginDefinition.class.isAssignableFrom(item.getKey())) {
                    UIPluginDefinition toImport = item.getTypedValue();
                    if (!pluginDefService.retrievePlugin(toImport.getName()).isPresent()) {
                        pluginDefService.savePlugin(toImport);
                    } else {
                        importErrors.add(String.format("plugin definition  %s not imported as already exists",
                                                       toImport.getName()));
                    }
                } else if (UIPluginDefConfAssociation.class.isAssignableFrom(item.getKey())) {
                    UIPluginDefConfAssociation toImport = item.getTypedValue();
                    Optional<UIPluginDefinition> def = pluginDefService.retrievePlugin(toImport.getPluginDefName());
                    if (def.isPresent()) {
                        UIPluginConfiguration conf = toImport.getPluginConf();
                        conf.setPluginDefinition(def.get());
                        pluginConfService.createPluginconfiguration(conf);
                    } else {
                        importErrors
                                .add(String.format("Plugin configuration can not be saved as plugin {} does not exists",
                                                   toImport.getPluginDefName()));
                    }
                }
            } catch (EntityException e) {
                LOGGER.error(e.getMessage(), e);
                importErrors.add(e.getMessage());
            }
        }
        return importErrors;
    }

    @Override
    public ModuleReadinessReport<Void> isReady() {
        return new ModuleReadinessReport<Void>(true, null, null);
    }

}
