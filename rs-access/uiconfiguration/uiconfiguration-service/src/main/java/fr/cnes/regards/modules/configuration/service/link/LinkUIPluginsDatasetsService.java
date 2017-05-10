/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service.link;

import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.dao.ILinkUIPluginsDatasetsRepository;
import fr.cnes.regards.modules.configuration.domain.LinkUIPluginsDatasets;

/**
 * Service handling properly how the mapping of plugin configurations to datasets is done.
 *
 * @author Sébastien Binda
 */
@Service
@Transactional
public class LinkUIPluginsDatasetsService implements ILinkUIPluginsDatasetsService {

    private final ILinkUIPluginsDatasetsRepository linkRepo;

    /**
     * Constructor
     *
     * @param pLinkRepo
     *            the repository handling {@link LinkPluginsDatasets}
     * @param pDatasetClient
     *            Feign client providing {@link Dataset}s
     */
    public LinkUIPluginsDatasetsService(final ILinkUIPluginsDatasetsRepository pLinkRepo) {
        super();
        linkRepo = pLinkRepo;
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
            if (pUpdatedLink.getServices().isEmpty()) {
                linkRepo.delete(existingOne);
                return null;
            } else {
                existingOne.setServices(pUpdatedLink.getServices());
                return linkRepo.save(existingOne);
            }
        } else {
            return linkRepo.save(pUpdatedLink);
        }
    }

}
