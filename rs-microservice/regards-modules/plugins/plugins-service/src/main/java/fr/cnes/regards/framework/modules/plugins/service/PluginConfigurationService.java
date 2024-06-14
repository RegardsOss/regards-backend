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
package fr.cnes.regards.framework.modules.plugins.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.NestedPluginParam;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.PluginParamType;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service to interact with the {@link IPluginConfigurationRepository} repository
 */
@Service
public class PluginConfigurationService {

    /**
     * {@link PluginConfiguration} JPA Repository
     */
    private final IPluginConfigurationRepository pluginConfigurationRepository;

    public PluginConfigurationService(IPluginConfigurationRepository pluginConfigurationRepository) {
        this.pluginConfigurationRepository = pluginConfigurationRepository;
    }

    // ------------
    // --- FIND ---
    // ------------

    @MultitenantTransactional(readOnly = true)
    public PluginConfiguration findCompleteByBusinessId(String businessId) {
        return pluginConfigurationRepository.findCompleteByBusinessId(businessId);
    }

    @MultitenantTransactional(readOnly = true)
    public List<PluginConfiguration> findAllPluginConfigurations() {
        return pluginConfigurationRepository.findAll();
    }

    public List<PluginConfiguration> findAllPluginConfigurationsSorted(Sort sort) {
        return pluginConfigurationRepository.findAll(sort);
    }

    @MultitenantTransactional(readOnly = true)
    public List<PluginConfiguration> findByPluginIdOrderByPriorityOrderDesc(String pluginId) {
        return pluginConfigurationRepository.findByPluginIdOrderByPriorityOrderDesc(pluginId);
    }

    @MultitenantTransactional(readOnly = true)
    public List<PluginConfiguration> findByPluginIdAndActiveTrueOrderByPriorityOrderDesc(String pluginId) {
        return pluginConfigurationRepository.findByPluginIdAndActiveTrueOrderByPriorityOrderDesc(pluginId);
    }

    @MultitenantTransactional(readOnly = true)
    public boolean existsByBusinessId(String businessId) {
        return pluginConfigurationRepository.existsByBusinessId(businessId);
    }

    @MultitenantTransactional(readOnly = true)
    public PluginConfiguration findOneByLabel(String configurationLabel) {
        return pluginConfigurationRepository.findOneByLabel(configurationLabel);
    }

    @MultitenantTransactional(readOnly = true)
    public Set<PluginConfiguration> getDependentPlugins(String businessId) {
        Set<PluginConfiguration> dependents = new HashSet<>();
        for (PluginConfiguration conf : this.findAllPluginConfigurations()) {
            for (IPluginParam param : conf.getParameters()) {
                if (param.getType() == PluginParamType.PLUGIN) {
                    NestedPluginParam nested = (NestedPluginParam) param;
                    if (businessId.equals(nested.getValue())) {
                        dependents.add(conf);
                    }
                }
            }
        }
        return dependents;
    }


    // --------------
    // --- UPDATE ---
    // --------------

    @MultitenantTransactional
    public PluginConfiguration savePluginConfiguration(PluginConfiguration pluginConfiguration) {
        return pluginConfigurationRepository.save(pluginConfiguration);
    }

    // --------------
    // --- DELETE ---
    // --------------

    @MultitenantTransactional
    public void deleteById(Long id) {
        pluginConfigurationRepository.deleteById(id);
    }

}
