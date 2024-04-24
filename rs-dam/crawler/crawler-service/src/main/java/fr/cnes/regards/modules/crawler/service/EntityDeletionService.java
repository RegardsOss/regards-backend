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
package fr.cnes.regards.modules.crawler.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.crawler.dao.IDeletionRequestRepository;
import fr.cnes.regards.modules.crawler.domain.EntityDeletionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service used to run {@link EntityDeletionRequest}s, which delete indexed data objects from elasticsearch.
 *
 * @author Thibaud Michaudel
 **/
@Service
public class EntityDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityDeletionService.class);

    private final IngesterService ingesterService;

    private final EntityIndexerService entityIndexerService;

    private final IDeletionRequestRepository deletionRequestRepository;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public EntityDeletionService(IngesterService ingesterService,
                                 EntityIndexerService entityIndexerService,
                                 IDeletionRequestRepository deletionRequestRepository,
                                 IRuntimeTenantResolver runtimeTenantResolver) {
        this.ingesterService = ingesterService;
        this.entityIndexerService = entityIndexerService;
        this.deletionRequestRepository = deletionRequestRepository;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    /**
     * Create a {@link EntityDeletionRequest} for each given entityId.
     */
    @MultitenantTransactional
    public void createRequests(List<String> entitiesIds) {
        LOGGER.info("Saving {} new entity deletion requests", entitiesIds.size());
        deletionRequestRepository.saveAll(entitiesIds.stream().map(EntityDeletionRequest::new).toList());
    }

    /**
     * Handle all {@link EntityDeletionRequest}s if there is no running ingestion (using
     * {@link IngesterService#lockIngestion()}. The requests are deleted after completion.
     */
    @MultitenantTransactional
    public Pageable handleEntityDeletion(Pageable page) {
        Page<EntityDeletionRequest> requestsPage = deletionRequestRepository.findAll(page);
        if (requestsPage.hasContent()) {
            List<EntityDeletionRequest> requests = requestsPage.getContent();
            LOGGER.debug("Deleting {} entities", requests.size());
            entityIndexerService.deleteDataObjectsAndUpdate(runtimeTenantResolver.getTenant(),
                                                            requests.stream()
                                                                    .map(EntityDeletionRequest::getEntityId)
                                                                    .collect(Collectors.toSet()));
            LOGGER.debug("{} entities deleted", requests.size());
            deletionRequestRepository.deleteAllInBatch(requests);
        }
        return requestsPage.nextPageable();
    }
}
