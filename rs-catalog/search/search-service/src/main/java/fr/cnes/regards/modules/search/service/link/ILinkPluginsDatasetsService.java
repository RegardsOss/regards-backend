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

    LinkPluginsDatasets retrieveLink(Long pDatasetId) throws EntityNotFoundException;

    LinkPluginsDatasets updateLink(Long pDatasetId, LinkPluginsDatasets pUpdatedLink) throws EntityNotFoundException;

}