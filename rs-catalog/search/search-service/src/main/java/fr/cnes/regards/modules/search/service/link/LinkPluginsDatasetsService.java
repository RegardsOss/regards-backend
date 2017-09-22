/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service.link;

import javax.annotation.PostConstruct;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.event.BroadcastEntityEvent;
import fr.cnes.regards.modules.entities.domain.event.EventType;
import fr.cnes.regards.modules.search.dao.ILinkPluginsDatasetsRepository;
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;

/**
 * Service handling properly how the mapping of plugin configurations to datasets is done.
 *
 * @author Sylvain Vissiere-Guerinet
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
     * JPA Respository to access {@link LinkPluginsDatasets} entities
     */
    private final ILinkPluginsDatasetsRepository linkRepo;

    /**
     * Constructor
     *
     * @param pLinkRepo
     *            the repository handling {@link LinkPluginsDatasets}
     * @param pDatasetClient
     *            Feign client providing {@link Dataset}s
     */
    public LinkPluginsDatasetsService(final IRuntimeTenantResolver pRunTimeTenantResolver,
            final ISubscriber pSubscriber, final ILinkPluginsDatasetsRepository pLinkRepo) {
        super();
        runtimeTenantResolver = pRunTimeTenantResolver;
        subscriber = pSubscriber;
        linkRepo = pLinkRepo;
    }

    @PostConstruct
    public void init() {
        // Subscribe to entity events in order to delete links to deleted dataset.
        subscriber.subscribeTo(BroadcastEntityEvent.class, new DeleteEntityEventHandler());
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
                for (final UniformResourceName ipId : pWrapper.getContent().getIpIds()) {
                    final LinkPluginsDatasets link = linkRepo.findOneByDatasetId(ipId.toString());
                    if (link != null) {
                        linkRepo.delete(link);
                    }
                }
            }
        }
    }

    /**
     * @param pDatasetId
     * @return
     * @throws EntityNotFoundException
     */
    @Override
    public LinkPluginsDatasets retrieveLink(final String pDatasetId) throws EntityNotFoundException {

        final LinkPluginsDatasets linkPluginsDatasets = linkRepo.findOneByDatasetId(pDatasetId);
        if (linkPluginsDatasets == null) {
            return linkRepo.save(new LinkPluginsDatasets(pDatasetId, Sets.newHashSet()));
        }
        return linkPluginsDatasets;
    }

    /**
     * @param pDatasetId
     * @param pUpdatedLink
     * @return
     * @throws EntityNotFoundException
     * @throws EntityInvalidException
     */
    @Override
    public LinkPluginsDatasets updateLink(final String pDatasetId, final LinkPluginsDatasets pUpdatedLink)
            throws EntityNotFoundException, EntityInvalidException {

        if (!pDatasetId.equals(pUpdatedLink.getDatasetId())) {
            throw new EntityInvalidException(String.format("Invalid datasetId %s ", pDatasetId));
        }

        // If exists retrieve previous link associated to the same datasetid
        final LinkPluginsDatasets existingOne = linkRepo.findOneByDatasetId(pDatasetId);
        if (existingOne != null) {
            existingOne.setServices(pUpdatedLink.getServices());
            return linkRepo.save(existingOne);
        } else {
            return linkRepo.save(pUpdatedLink);
        }
    }

}
