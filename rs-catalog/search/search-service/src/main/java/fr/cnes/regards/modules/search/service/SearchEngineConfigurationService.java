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
package fr.cnes.regards.modules.search.service;

import com.google.common.collect.Lists;
import feign.FeignException;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.client.entities.IDatasetClient;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.event.BroadcastEntityEvent;
import fr.cnes.regards.modules.dam.domain.entities.event.EventType;
import fr.cnes.regards.modules.search.dao.ISearchEngineConfRepository;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineConfiguration;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service to handle {@link SearchEngineConfiguration} entities.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class SearchEngineConfigurationService implements ISearchEngineConfigurationService, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngineConfigurationService.class);

    public static final String LEGACY_SEARCH_ENGINE_BUSINESS_ID = "search-engine-legacy";

    @Autowired
    private ISearchEngineConfRepository repository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IDatasetClient datasetClient;

    @Override
    public void afterPropertiesSet() {
        // Subscribe to entity events in order to delete links to deleted dataset.
        subscriber.subscribeTo(BroadcastEntityEvent.class, new DeleteEntityEventHandler());
    }

    @Override
    public void initDefaultSearchEngine(Class<?> legacySearchEnginePluginClass) {
        // Initialize the mandatory legacy searchengine if it does not exists yet.
        SearchEngineConfiguration conf = repository.findByDatasetUrnIsNullAndConfigurationPluginId(SearchEngineMappings.LEGACY_PLUGIN_ID);
        if (conf == null) {
            // Create the new one
            conf = new SearchEngineConfiguration();
            conf.setLabel("REGARDS search protocol");
            PluginConfiguration pluginConf = PluginConfiguration.build(legacySearchEnginePluginClass,
                                                                       null,
                                                                       IPluginParam.set());
            pluginConf.setBusinessId(LEGACY_SEARCH_ENGINE_BUSINESS_ID);
            pluginConf.setLabel(LEGACY_SEARCH_ENGINE_BUSINESS_ID);
            conf.setConfiguration(pluginConf);
            try {
                createConf(conf);
            } catch (ModuleException e) {
                LOGGER.error("Error initializing legacy search engine", e);
            }
        }
    }

    @Override
    public SearchEngineConfiguration createConf(SearchEngineConfiguration conf) throws ModuleException {
        // First check if associated conf exists.
        if (conf.getConfiguration() == null) {
            throw new EntityInvalidException("Plugin configuration can not be null for new SearchEngineConfiguration");
        } else {
            // Check if a conf does already exists for the given engine and dataset
            checkConfAlreadyExists(conf);
            if (conf.getConfiguration().getId() == null) {
                // If plugin conf is a new one create it first.
                conf.setConfiguration(pluginService.savePluginConfiguration(conf.getConfiguration()));
            }
        }
        return addDatasetLabel(repository.save(conf), Lists.newArrayList());
    }

    @Override
    public SearchEngineConfiguration updateConf(SearchEngineConfiguration conf) throws ModuleException {
        if (conf.getId() == null) {
            throw new EntityOperationForbiddenException("Cannot update entity does not exists");
        } else {
            retrieveConf(conf.getId());
            checkConfAlreadyExists(conf);
        }
        // First save plugin conf update
        pluginService.updatePluginConfiguration(conf.getConfiguration());
        // Then save engine conf
        return addDatasetLabel(repository.save(conf), Lists.newArrayList());
    }

    @Override
    public void deleteConf(Long confId) throws ModuleException {
        SearchEngineConfiguration confToDelete = retrieveConf(confId);
        repository.deleteById(confId);
        // If after deleting conf, no other reference the associated pluginConf so delete it too.
        Page<SearchEngineConfiguration> otherConfs = repository.findByConfigurationId(confToDelete.getConfiguration()
                                                                                                  .getId(),
                                                                                      PageRequest.of(0, 1));
        if (otherConfs.getContent().isEmpty()) {
            pluginService.deletePluginConfiguration(confToDelete.getConfiguration().getBusinessId());
        }

    }

    @Override
    public SearchEngineConfiguration retrieveConf(Long confId) throws ModuleException {
        Optional<SearchEngineConfiguration> conf = repository.findById(confId);
        if (!conf.isPresent()) {
            throw new EntityNotFoundException(confId, SearchEngineConfiguration.class);
        }
        return addDatasetLabel(conf.get(), Lists.newArrayList());
    }

    @Override
    public SearchEngineConfiguration retrieveConf(Optional<UniformResourceName> datasetUrn, String pluginId)
        throws ModuleException {
        SearchEngineConfiguration conf = null;
        String ds = null;
        if (datasetUrn.isPresent()) {
            ds = datasetUrn.get().toString();
            conf = repository.findByDatasetUrnAndConfigurationPluginId(datasetUrn.get().toString(), pluginId);
        }
        // If no conf found, then get a conf without dataset for the given pluginId
        if (conf == null) {
            conf = repository.findByDatasetUrnIsNullAndConfigurationPluginId(pluginId);
        }

        if (conf == null) {
            throw new EntityNotFoundException(String.format("SearchType=%s and Dataset=%s", pluginId, ds),
                                              SearchEngineConfiguration.class);
        }

        return addDatasetLabel(conf, Lists.newArrayList());
    }

    /**
     * Check if a conf does already exists for the given engine and dataset
     *
     * @param conf
     * @throws EntityInvalidException
     */
    private void checkConfAlreadyExists(SearchEngineConfiguration conf) throws EntityInvalidException {
        SearchEngineConfiguration foundConf = null;
        if (conf.getDatasetUrn() != null) {
            foundConf = repository.findByDatasetUrnAndConfigurationPluginId(conf.getDatasetUrn(),
                                                                            conf.getConfiguration().getPluginId());

        } else {
            foundConf = repository.findByDatasetUrnIsNullAndConfigurationPluginId(conf.getConfiguration()
                                                                                      .getPluginId());
        }

        if ((foundConf != null) && !foundConf.getId().equals(conf.getId())) {
            throw new EntityInvalidException(String.format(
                "Search engine already defined for engine <%s> and dataset <%s>",
                conf.getConfiguration().getPluginId(),
                conf.getDatasetUrn()));
        }
    }

    @Override
    public List<SearchEngineConfiguration> retrieveAllConfs() {
        return repository.findAll();
    }

    @Override
    public Page<SearchEngineConfiguration> retrieveConfs(Optional<String> engineType, Pageable page) {
        Page<SearchEngineConfiguration> confs;
        if (engineType.isPresent()) {
            confs = repository.findByConfigurationPluginId(engineType.get(), page);
        } else {
            confs = repository.findAll(page);
        }
        List<Dataset> cachedDatasets = Lists.newArrayList();
        confs.getContent().forEach(c -> addDatasetLabel(c, cachedDatasets));
        return confs;
    }

    public SearchEngineConfiguration addDatasetLabel(SearchEngineConfiguration conf, List<Dataset> cachedDatasets) {
        if (conf.getDatasetUrn() != null) {
            // First check if dataset is already in cache
            Optional<Dataset> oDs = cachedDatasets.stream()
                                                  .filter(ds -> ds.getIpId().toString().equals(conf.getDatasetUrn()))
                                                  .findFirst();
            if (oDs.isPresent()) {
                conf.setDataset(oDs.get());
            } else {
                // Retrieve dataset from dam
                try {
                    FeignSecurityManager.asSystem();
                    ResponseEntity<EntityModel<Dataset>> response = datasetClient.retrieveDataset(conf.getDatasetUrn());
                    if ((response != null) && (response.getBody() != null) && (response.getBody().getContent()
                        != null)) {
                        conf.setDataset(response.getBody().getContent());
                        // Add new retrieved dataset into cached ones
                        cachedDatasets.add(response.getBody().getContent());
                    }
                } catch (FeignException e) {
                    LOGGER.error(String.format("Error retrieving dataset with ipId %s", conf.getDatasetUrn()), e);
                } finally {
                    FeignSecurityManager.reset();
                }
            }
        }
        return conf;
    }

    /**
     * Class DeleteEntityEventHandler
     * Handler to delete {@link SearchEngineConfiguration} for deleted datasets.
     *
     * @author Sébastien Binda
     */
    private class DeleteEntityEventHandler implements IHandler<BroadcastEntityEvent> {

        @Override
        public void handle(final TenantWrapper<BroadcastEntityEvent> pWrapper) {
            if ((pWrapper.getContent() != null) && EventType.DELETE.equals(pWrapper.getContent().getEventType())) {
                runtimeTenantResolver.forceTenant(pWrapper.getTenant());
                for (final UniformResourceName ipId : pWrapper.getContent().getAipIds()) {
                    List<SearchEngineConfiguration> confs = repository.findByDatasetUrn(ipId.toString());
                    if ((confs != null) && !confs.isEmpty()) {
                        confs.forEach(repository::delete);
                    }
                }
            }
        }
    }
}
