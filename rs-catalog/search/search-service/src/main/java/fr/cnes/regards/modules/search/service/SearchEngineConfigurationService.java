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
package fr.cnes.regards.modules.search.service;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.assertj.core.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import feign.FeignException;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.event.BroadcastEntityEvent;
import fr.cnes.regards.modules.entities.domain.event.EventType;
import fr.cnes.regards.modules.search.dao.ISearchEngineConfRepository;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineConfiguration;

/**
 * Service to handle {@link SearchEngineConfiguration} entities.
 * @author Sébastien Binda
 *
 */
@Service
@MultitenantTransactional
public class SearchEngineConfigurationService implements ISearchEngineConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngineConfigurationService.class);

    @Autowired
    private ISearchEngineConfRepository repository;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IDatasetClient datasetClient;

    @PostConstruct
    public void listenForDatasetEvents() {
        // Subscribe to entity events in order to delete links to deleted dataset.
        subscriber.subscribeTo(BroadcastEntityEvent.class, new DeleteEntityEventHandler());
    }

    @Override
    public SearchEngineConfiguration createConf(SearchEngineConfiguration conf) {
        return addDatasetLabel(repository.save(conf), Lists.newArrayList());
    }

    @Override
    public SearchEngineConfiguration updateConf(SearchEngineConfiguration conf) throws ModuleException {
        if (conf.getId() == null) {
            throw new EntityOperationForbiddenException("Cannot update entity does not exists");
        } else {
            retrieveConf(conf.getId());
        }
        return addDatasetLabel(repository.save(conf), Lists.newArrayList());
    }

    @Override
    public void deleteConf(Long confId) throws ModuleException {
        retrieveConf(confId);
        repository.delete(confId);
    }

    @Override
    public SearchEngineConfiguration retrieveConf(Long confId) throws ModuleException {
        SearchEngineConfiguration conf = repository.findOne(confId);
        if (conf == null) {
            throw new EntityNotFoundException(confId, SearchEngineConfiguration.class);
        }
        return addDatasetLabel(conf, Lists.newArrayList());
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
    * Class DeleteEntityEventHandler
    * Handler to delete {@link SearchEngineConfiguration} for deleted datasets.
    * @author Sébastien Binda
    */
    private class DeleteEntityEventHandler implements IHandler<BroadcastEntityEvent> {

        @Override
        public void handle(final TenantWrapper<BroadcastEntityEvent> pWrapper) {
            if ((pWrapper.getContent() != null) && EventType.DELETE.equals(pWrapper.getContent().getEventType())) {
                runtimeTenantResolver.forceTenant(pWrapper.getTenant());
                for (final UniformResourceName ipId : pWrapper.getContent().getIpIds()) {
                    List<SearchEngineConfiguration> confs = repository.findByDatasetUrn(ipId.toString());
                    if ((confs != null) && !confs.isEmpty()) {
                        confs.forEach(repository::delete);
                    }
                }
            }
        }
    }

    @Override
    public Page<SearchEngineConfiguration> retrieveConfs(Optional<String> engineType, Pageable page)
            throws ModuleException {
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
                    .filter(ds -> ds.getIpId().toString().equals(conf.getDatasetUrn())).findFirst();
            if (oDs.isPresent()) {
                conf.setDataset(oDs.get());
            } else {
                // Retrieve dataset from dam
                try {
                    ResponseEntity<Resource<Dataset>> response = datasetClient.retrieveDataset(conf.getDatasetUrn());
                    if ((response != null) && (response.getBody() != null)
                            && (response.getBody().getContent() != null)) {
                        conf.setDataset(response.getBody().getContent());
                        // Add new retrieved dataset into cached ones
                        cachedDatasets.add(response.getBody().getContent());
                    }
                } catch (FeignException e) {
                    LOGGER.error(String.format("Error retrieving dataset with ipId %s", conf.getDatasetUrn()), e);
                }
            }
        }
        return conf;
    }
}
