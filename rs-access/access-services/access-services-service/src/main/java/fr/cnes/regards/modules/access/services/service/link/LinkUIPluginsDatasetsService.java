/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.service.link;

import java.util.ArrayList;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.access.services.dao.ui.ILinkUIPluginsDatasetsRepository;
import fr.cnes.regards.modules.access.services.domain.event.LinkUiPluginsDatasetsEvent;
import fr.cnes.regards.modules.access.services.domain.ui.LinkUIPluginsDatasets;
import fr.cnes.regards.modules.dam.domain.entities.event.BroadcastEntityEvent;
import fr.cnes.regards.modules.dam.domain.entities.event.EventType;

/**
 * Service handling properly how the mapping of plugin configurations to datasets is done.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */
@Service
@RegardsTransactional
public class LinkUIPluginsDatasetsService implements ILinkUIPluginsDatasetsService {

    /**
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private ILinkUIPluginsDatasetsRepository linkRepo;

    /**
     * Post-construct initialization
     */
    @PostConstruct
    public void init() {
        // Subscribe to entity events in order to delete links to deleted dataset.
        subscriber.subscribeTo(BroadcastEntityEvent.class, new DeleteEntityEventHandler());
    }

    /**
     * @param pDatasetId
     * @return {@link LinkUIPluginsDatasets}
     * @throws EntityNotFoundException
     */
    @Override
    public LinkUIPluginsDatasets retrieveLink(final String pDatasetId) throws EntityNotFoundException {
        if (linkRepo.findOneByDatasetId(pDatasetId) != null) {
            return linkRepo.findOneByDatasetId(pDatasetId);
        }
        return createLink(pDatasetId);
    }

    /**
     * @param pDatasetId
     * @param pUpdatedLink
     * @return {@link LinkUIPluginsDatasets}
     * @throws EntityNotFoundException
     * @throws EntityInvalidException
     */
    @Override
    public LinkUIPluginsDatasets updateLink(final String pDatasetId, final LinkUIPluginsDatasets pUpdatedLink)
            throws EntityNotFoundException, EntityInvalidException {
        if (!pDatasetId.equals(pUpdatedLink.getDatasetId())) {
            throw new EntityInvalidException(String.format("Invalid datasetId %s ", pDatasetId));
        }

        // If exists retrieve previous link associated to the same datasetid
        final LinkUIPluginsDatasets existingOne = linkRepo.findOneByDatasetId(pDatasetId);
        if (existingOne != null) {
            existingOne.setServices(pUpdatedLink.getServices());
            LinkUIPluginsDatasets saved = linkRepo.save(existingOne);
            publisher.publish(new LinkUiPluginsDatasetsEvent(saved));
            return saved;
        } else {
            return createLink(pUpdatedLink);
        }
    }

    /**
     * Delete a link
     * @param pLinkUIPluginsDatasets the link to delete
     */
    private void deleteLink(LinkUIPluginsDatasets pLinkUIPluginsDatasets) { // NOSONAR
        linkRepo.delete(pLinkUIPluginsDatasets);
        publisher.publish(new LinkUiPluginsDatasetsEvent(pLinkUIPluginsDatasets));
    }

    /**
     * Save a new link in db
     *
     * @param pDatasetId
     * @return the created link
     */
    private LinkUIPluginsDatasets createLink(String pDatasetId) {
        LinkUIPluginsDatasets newLink = linkRepo.save(new LinkUIPluginsDatasets(pDatasetId, new ArrayList<>()));
        publisher.publish(new LinkUiPluginsDatasetsEvent(newLink));
        return newLink;
    }

    /**
     * Save a new link in db
     *
     * @param pLinkUIPluginsDatasets
     * @return the created link
     */
    private LinkUIPluginsDatasets createLink(LinkUIPluginsDatasets pLinkUIPluginsDatasets) {
        LinkUIPluginsDatasets newLink = linkRepo.save(pLinkUIPluginsDatasets);
        publisher.publish(new LinkUiPluginsDatasetsEvent(newLink));
        return newLink;
    }

    private class DeleteEntityEventHandler implements IHandler<BroadcastEntityEvent> {

        @Override
        public void handle(final TenantWrapper<BroadcastEntityEvent> wrapper) {
            if ((wrapper.getContent() != null) && (wrapper.getContent().getEventType() == EventType.DELETE)) {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                for (final UniformResourceName ipId : wrapper.getContent().getAipIds()) {
                    final LinkUIPluginsDatasets link = linkRepo.findOneByDatasetId(ipId.toString());
                    if (link != null) {
                        deleteLink(link);
                    }
                }
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
