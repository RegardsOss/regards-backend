/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.link;

import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.search.dao.ILinkPluginsDatasetsRepository;
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;

/**
 * Service handling properly how the mapping of plugin configurations to datasets is done.
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Service
public class LinkPluginsDatasetsService implements ILinkPluginsDatasetsService {

    private final ILinkPluginsDatasetsRepository linkRepo;

    /**
     * Constructor
     *
     * @param pLinkRepo
     *            the repository handling {@link LinkPluginsDatasets}
     * @param pDatasetClient
     *            Feign client providing {@link Dataset}s
     */
    public LinkPluginsDatasetsService(final ILinkPluginsDatasetsRepository pLinkRepo,
            final IDatasetClient pDatasetClient) {
        super();
        linkRepo = pLinkRepo;
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
