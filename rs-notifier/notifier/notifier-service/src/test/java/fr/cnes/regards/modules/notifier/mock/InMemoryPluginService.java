/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.mock;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class InMemoryPluginService implements IPluginService {

    private final List<PluginConfiguration> savedPlugins;

    public InMemoryPluginService() {
        savedPlugins = new ArrayList<>();
    }

    @Override
    public Set<String> getPluginTypes() {
        throw new NotImplementedException("tbd");
    }

    @Override
    public Set<String> getAvailablePluginTypes() {
        throw new NotImplementedException("tbd");
    }

    @Override
    public List<PluginMetaData> getPlugins() {
        throw new NotImplementedException("tbd");
    }

    @Override
    public List<PluginMetaData> getPluginsByType(Class<?> interfacePluginType) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public boolean canInstantiate(String businessId) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public <T> T getPlugin(String businessId, IPluginParam... dynamicPluginParameters) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public <T> T getPlugin(PluginConfiguration plgConf, IPluginParam... dynamicPluginParameters)
        throws ModuleException, NotAvailablePluginConfigurationException {
        throw new NotImplementedException("tbd");
    }

    @Override
    public <T> Optional<T> getOptionalPlugin(String businessId, IPluginParam... dynamicPluginParameters) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public <T> T getFirstPluginByType(Class<?> interfacePluginType, IPluginParam... pluginParameters) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public PluginMetaData getPluginMetaDataById(String pluginImplId) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public PluginConfiguration savePluginConfiguration(PluginConfiguration pluginConf) {
        pluginConf.setId(1L);
        try {
            deletePluginConfiguration(pluginConf.getBusinessId());
        } catch (ModuleException e) {
            // No possible error => ignore
        }
        savedPlugins.add(pluginConf);
        return pluginConf;
    }

    @Override
    public void deletePluginConfiguration(String businessId) throws EntityNotFoundException,
        EntityOperationForbiddenException {
        savedPlugins.removeIf(p -> p.getBusinessId().equals(businessId));
    }

    @Override
    public PluginConfiguration updatePluginConfiguration(PluginConfiguration plugin) {
        return savePluginConfiguration(plugin);
    }

    @Override
    public PluginConfiguration getPluginConfiguration(String businessId) throws EntityNotFoundException {
        return savedPlugins.stream()
                           .filter(plugin -> plugin.getBusinessId().equals(businessId))
                           .findFirst()
                           .orElseThrow(() -> new EntityNotFoundException("not found"));
    }

    @Override
    public PluginConfiguration loadPluginConfiguration(String businessId) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public void setMetadata(PluginConfiguration... pluginConfigurations) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public boolean exists(String businessId) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurationsByType(Class<?> interfacePluginType) {
        // No need to filter for tests
        // Improve if necessary (not that easy)
        return savedPlugins;
    }

    @Override
    public List<PluginConfiguration> getAllPluginConfigurations() {
        throw new NotImplementedException("tbd");
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurations(String pluginId) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public List<PluginConfiguration> getActivePluginConfigurations(String pluginId) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public PluginMetaData checkPluginClassName(Class<?> clazz, String pluginClassName) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public PluginConfiguration getPluginConfigurationByLabel(String configurationLabel) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public Optional<PluginConfiguration> findPluginConfigurationByLabel(String configurationLabel) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public void cleanLocalPluginCache(String businessId) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public void cleanPluginCache() {
        throw new NotImplementedException("tbd");
    }

    @Override
    public PluginConfiguration prepareForExport(PluginConfiguration pluginConf) {
        throw new NotImplementedException("tbd");
    }
}