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

import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;

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
    public boolean canInstantiate(Long id) throws ModuleException, NotAvailablePluginConfigurationException {
        throw new NotImplementedException("tbd");
    }

    @Override
    public boolean canInstantiate(String businessId) throws ModuleException, NotAvailablePluginConfigurationException {
        throw new NotImplementedException("tbd");
    }

    @Override
    public <T> T getPlugin(Long id, IPluginParam... dynamicPluginParameters)
        throws ModuleException, NotAvailablePluginConfigurationException {
        throw new NotImplementedException("tbd");
    }

    @Override
    public <T> T getPlugin(String businessId, IPluginParam... dynamicPluginParameters)
        throws ModuleException, NotAvailablePluginConfigurationException {
        throw new NotImplementedException("tbd");
    }

    @Override
    public <T> Optional<T> getOptionalPlugin(String businessId, IPluginParam... dynamicPluginParameters)
        throws NotAvailablePluginConfigurationException {
        throw new NotImplementedException("tbd");
    }

    @Override
    public <T> T getFirstPluginByType(Class<?> interfacePluginType, IPluginParam... pluginParameters)
        throws ModuleException, NotAvailablePluginConfigurationException {
        throw new NotImplementedException("tbd");
    }

    @Override
    public PluginMetaData getPluginMetaDataById(String pluginImplId) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public PluginConfiguration savePluginConfiguration(PluginConfiguration pluginConf)
        throws EntityInvalidException, EncryptionException, EntityNotFoundException {
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
    public void deletePluginConfiguration(String businessId) throws ModuleException {
        savedPlugins.removeIf(p -> p.getBusinessId().equals(businessId));
    }

    @Override
    public PluginConfiguration updatePluginConfiguration(PluginConfiguration plugin) throws ModuleException {
        return savePluginConfiguration(plugin);
    }

    @Override
    public PluginConfiguration getPluginConfiguration(Long id) throws EntityNotFoundException {
        throw new NotImplementedException("tbd");
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
    public boolean existsByLabel(String pluginConfLabel) {
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
    public PluginMetaData checkPluginClassName(Class<?> clazz, String pluginClassName) throws EntityInvalidException {
        throw new NotImplementedException("tbd");
    }

    @Override
    public PluginConfiguration getPluginConfigurationByLabel(String configurationLabel) throws EntityNotFoundException {
        throw new NotImplementedException("tbd");
    }

    @Override
    public Optional<PluginConfiguration> findPluginConfigurationByLabel(String configurationLabel) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public boolean isPluginCached(String businessId) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public void cleanPluginCache(String businessId) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public void cleanPluginCache() {
        throw new NotImplementedException("tbd");
    }

    @Override
    public Map<String, Object> getPluginCache() {
        throw new NotImplementedException("tbd");
    }

    @Override
    public Object getCachedPlugin(String businessId) {
        throw new NotImplementedException("tbd");
    }

    @Override
    public PluginConfiguration prepareForExport(PluginConfiguration pluginConf) {
        throw new NotImplementedException("tbd");
    }
}