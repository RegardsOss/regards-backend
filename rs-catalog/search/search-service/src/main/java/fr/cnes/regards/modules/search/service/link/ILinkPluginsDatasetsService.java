/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.link;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public interface ILinkPluginsDatasetsService {

    /**
     * @param pDatasetId
     * @return
     * @throws EntityNotFoundException
     */
    LinkPluginsDatasets retrieveLink(Long pDatasetId) throws EntityNotFoundException;

    /**
     * @param pDatasetId
     * @param pUpdatedLink
     * @return
     * @throws EntityNotFoundException
     */
    LinkPluginsDatasets updateLink(Long pDatasetId, LinkPluginsDatasets pUpdatedLink) throws EntityNotFoundException;

}