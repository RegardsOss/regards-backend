/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service.link;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.domain.LinkUIPluginsDatasets;

/**
 * This interface must be usefull somehow... Document it.
 *
 * @author Sébastien Binda
 */
public interface ILinkUIPluginsDatasetsService {

    /**
     * Retrieves a {@link LinkPluginsDatasets} from a dataset
     *
     * @param pDatasetId
     *            the given dataset id
     * @return the {@link LinkPluginsDatasets}
     * @throws EntityNotFoundException
     *             if the given dataset does not exist
     */
    LinkUIPluginsDatasets retrieveLink(String pDatasetId) throws EntityNotFoundException;

    /**
     * Update a {@link LinkPluginsDatasets}
     *
     * @param pDatasetId
     *            the dataset id
     * @param pUpdatedLink
     *            the {@link LinkPluginsDatasets} to update
     * @return the updated {@link LinkPluginsDatasets}
     * @throws EntityNotFoundException
     *             if the given dataset does not exist
     * @throws EntityInvalidException
     */
    LinkUIPluginsDatasets updateLink(String pDatasetId, LinkUIPluginsDatasets pUpdatedLink) throws EntityException;

}