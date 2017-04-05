/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.link;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;

/**
 * This interface must be usefull somehow... Document it.
 * @author Sylvain Vissiere-Guerinet
 */
public interface ILinkPluginsDatasetsService {

    /**
     * Retrieves a {@link LinkPluginsDatasets} from a dataset
     * @param pDatasetId the given dataset id
     * @return the {@link LinkPluginsDatasets}
     * @throws EntityNotFoundException if the given dataset does not exist
     */
    LinkPluginsDatasets retrieveLink(Long pDatasetId) throws EntityNotFoundException;

    /**
     * Update a {@link LinkPluginsDatasets}
     * @param pDatasetId the dataset id
     * @param pUpdatedLink the {@link LinkPluginsDatasets} to update
     * @return the updated {@link LinkPluginsDatasets}
     * @throws EntityNotFoundException if the given dataset does not exist
     */
    LinkPluginsDatasets updateLink(Long pDatasetId, LinkPluginsDatasets pUpdatedLink) throws EntityNotFoundException;

}