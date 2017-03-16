/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.link;

import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.search.dao.ILinkPluginsDatasetsRepository;
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;

/**
 * Service handling properly how the mapping of plugin configurations to datasets is done.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Service
public class LinkPluginsDatasetsService {

    private final ILinkPluginsDatasetsRepository linkRepo;

    private final IDatasetClient datasetClient;

    public LinkPluginsDatasetsService(ILinkPluginsDatasetsRepository pLinkRepo, IDatasetClient pDatasetClient) {
        super();
        linkRepo = pLinkRepo;
        datasetClient = pDatasetClient;
    }

    /**
     * @param pDatasetId
     * @return
     * @throws EntityNotFoundException
     */
    public LinkPluginsDatasets retrieveLink(Long pDatasetId) throws EntityNotFoundException {
        if (linkRepo.exists(pDatasetId)) {
            return linkRepo.findOne(pDatasetId);
        }
        if (!existsDataset(pDatasetId)) {
            throw new EntityNotFoundException(pDatasetId, Dataset.class);
        }
        return linkRepo
                .save(new LinkPluginsDatasets(pDatasetId, Sets.newHashSet(), Sets.newHashSet(), Sets.newHashSet()));
    }

    /**
     * @param pDatasetId
     * @return
     */
    private boolean existsDataset(Long pDatasetId) {
        FeignSecurityManager.asSystem();
        ResponseEntity<Resource<Dataset>> response = datasetClient.retrieveDataset(pDatasetId);
        FeignSecurityManager.reset();
        if (HttpUtils.isSuccess(response.getStatusCode())) {
            return true;
        }
        return false;
    }

    /**
     * @param pDatasetId
     * @param pUpdatedLink
     * @return
     * @throws EntityNotFoundException
     */
    public LinkPluginsDatasets updateLink(Long pDatasetId, LinkPluginsDatasets pUpdatedLink)
            throws EntityNotFoundException {
        if (!existsDataset(pDatasetId)) {
            throw new EntityNotFoundException(pDatasetId, Dataset.class);
        }
        return linkRepo.save(pUpdatedLink);
    }

}
