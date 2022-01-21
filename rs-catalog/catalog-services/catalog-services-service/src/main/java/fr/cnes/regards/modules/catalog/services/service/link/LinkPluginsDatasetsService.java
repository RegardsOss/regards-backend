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
package fr.cnes.regards.modules.catalog.services.service.link;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.catalog.services.dao.ILinkPluginsDatasetsRepository;
import fr.cnes.regards.modules.catalog.services.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.catalog.services.domain.event.LinkPluginsDatasetsEvent;
import fr.cnes.regards.modules.dam.domain.entities.event.BroadcastEntityEvent;
import fr.cnes.regards.modules.dam.domain.entities.event.EventType;

/**
 * Service handling properly how the mapping of plugin configurations to datasets is done.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Xavier-Alexandre Brochard
 */
@Service
@MultitenantTransactional
public class LinkPluginsDatasetsService implements ILinkPluginsDatasetsService {

    /**
     * Runtime tenant resolver
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * AMQ Subscriber
     */
    private final ISubscriber subscriber;

    /**
     * AMQ Publisher
     */
    private final IPublisher publisher;

    /**
     * JPA Respository to access {@link LinkPluginsDatasets} entities
     */
    private final ILinkPluginsDatasetsRepository linkRepo;

    /**
     * @param pRuntimeTenantResolver
     * @param pSubscriber
     * @param pPublisher
     * @param pLinkRepo
     */
    public LinkPluginsDatasetsService(IRuntimeTenantResolver pRuntimeTenantResolver, ISubscriber pSubscriber,
            IPublisher pPublisher, ILinkPluginsDatasetsRepository pLinkRepo) {
        super();
        runtimeTenantResolver = pRuntimeTenantResolver;
        subscriber = pSubscriber;
        publisher = pPublisher;
        linkRepo = pLinkRepo;
    }

    /**
     * Post-construct initialization
     */
    @PostConstruct
    public void init() {
        // Subscribe to entity events in order to delete links to deleted dataset.
        subscriber.subscribeTo(BroadcastEntityEvent.class, new DeleteEntityEventHandler());
    }

    @Override
    public LinkPluginsDatasets retrieveLink(final String pDatasetId) {
        Assert.notNull(pDatasetId, "Dataset id is required");
        final LinkPluginsDatasets linkPluginsDatasets = linkRepo.findOneByDatasetId(pDatasetId);
        if (linkPluginsDatasets == null) {
            return createLink(pDatasetId);
        }
        return linkPluginsDatasets;
    }

    @Override
    public LinkPluginsDatasets updateLink(final String pDatasetId, final LinkPluginsDatasets pUpdatedLink)
            throws EntityInvalidException {
        if (!pDatasetId.equals(pUpdatedLink.getDatasetId())) {
            throw new EntityInvalidException(String.format("Invalid datasetId %s ", pDatasetId));
        }

        // If exists retrieve previous link associated to the same datasetid
        final LinkPluginsDatasets existingOne = linkRepo.findOneByDatasetId(pDatasetId);
        if (existingOne == null) {
            return createLink(pUpdatedLink);
        } else {
            existingOne.setServices(pUpdatedLink.getServices());
            LinkPluginsDatasets saved = linkRepo.save(existingOne);
            publisher.publish(new LinkPluginsDatasetsEvent(saved));
            return saved;
        }
    }

    /**
     * Create a new link in db with given values
     *
     * @param pLink the values for the new link
     * @return the created link
     */
    private LinkPluginsDatasets createLink(final LinkPluginsDatasets pLink) {
        LinkPluginsDatasets newLink = linkRepo.save(pLink);
        publisher.publish(new LinkPluginsDatasetsEvent(newLink));
        return newLink;
    }

    /**
     * Create a new link in db with given data id
     *
     * @param pDatasetId the value of the dataset id to init the link with. Must not be null
     * @return the created link
     */
    private LinkPluginsDatasets createLink(final String pDatasetId) {
        Assert.notNull(pDatasetId, "Dataset id is required");
        LinkPluginsDatasets newLink = linkRepo.save(new LinkPluginsDatasets(pDatasetId, Sets.newHashSet()));
        publisher.publish(new LinkPluginsDatasetsEvent(newLink));
        return newLink;
    }

    /**
     * Dete the given link
     *
     * @param pLink the link to delete
     */
    private void deleteLink(final LinkPluginsDatasets pLink) { // NOSONAR
        publisher.publish(new LinkPluginsDatasetsEvent(pLink));
        linkRepo.delete(pLink);
    }

    /**
     *
     * Class DeleteEntityEventHandler
     *
     * Handler to delete {@link LinkPluginsDatasets} for deleted datasets.
     *
     * @author Sébastien Binda
     * @since 1.0-SNAPSHOT
     */
    private class DeleteEntityEventHandler implements IHandler<BroadcastEntityEvent> {

        @Override
        public void handle(final TenantWrapper<BroadcastEntityEvent> pWrapper) {
            if ((pWrapper.getContent() != null) && EventType.DELETE.equals(pWrapper.getContent().getEventType())) {
                runtimeTenantResolver.forceTenant(pWrapper.getTenant());
                for (final UniformResourceName ipId : pWrapper.getContent().getAipIds()) {
                    final LinkPluginsDatasets link = linkRepo.findOneByDatasetId(ipId.toString());
                    if (link != null) {
                        deleteLink(link);
                    }
                }
            }
        }

    }

}
