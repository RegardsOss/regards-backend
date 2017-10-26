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
package fr.cnes.regards.modules.access.services.service.link;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.access.services.domain.ui.LinkUIPluginsDatasets;

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