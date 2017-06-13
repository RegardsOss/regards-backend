/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service.link;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.configuration.dao.ILinkUIPluginsDatasetsRepository;
import fr.cnes.regards.modules.configuration.domain.LinkUIPluginsDatasets;
import fr.cnes.regards.modules.entities.domain.event.BroadcastEntityEvent;
import fr.cnes.regards.modules.entities.domain.event.EventType;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;

/**
 * Service handling properly how the mapping of plugin configurations to datasets is done.
 *
 * @author Sébastien Binda
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
    private ILinkUIPluginsDatasetsRepository linkRepo;

    @PostConstruct
    public void init() {
        // Subscribe to entity events in order to delete links to deleted dataset.
        subscriber.subscribeTo(BroadcastEntityEvent.class, new DeleteEntityEventHandler());
    }

    private class DeleteEntityEventHandler implements IHandler<BroadcastEntityEvent> {

        @Override
        public void handle(final TenantWrapper<BroadcastEntityEvent> pWrapper) {
            if ((pWrapper.getContent() != null) && EventType.DELETE.equals(pWrapper.getContent().getEventType())) {
                runtimeTenantResolver.forceTenant(pWrapper.getTenant());
                for (final UniformResourceName ipId : pWrapper.getContent().getIpIds()) {
                    final LinkUIPluginsDatasets link = linkRepo.findOneByDatasetId(ipId.toString());
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
    public LinkUIPluginsDatasets retrieveLink(final String pDatasetId) throws EntityNotFoundException {
        if (linkRepo.findOneByDatasetId(pDatasetId) != null) {
            return linkRepo.findOneByDatasetId(pDatasetId);
        }
        return linkRepo.save(new LinkUIPluginsDatasets(pDatasetId, new ArrayList<>()));
    }

    /**
     * @param pDatasetId
     * @param pUpdatedLink
     * @return
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
            return linkRepo.save(existingOne);
        } else {
            return linkRepo.save(pUpdatedLink);
        }
    }

}
