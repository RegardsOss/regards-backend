/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.configuration.service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.domain.Layout;

/**
 *
 * Class ILayoutService
 *
 * Service to manage layout configuration entities.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RegardsTransactional
public interface ILayoutService {

    /**
     *
     * Retrieve an application layout configuration by is applicationId
     *
     * @param applicationId
     * @return Layout
     * @throws EntityNotFoundException
     * @since 1.0-SNAPSHOT
     */
    Layout retrieveLayout(String applicationId) throws EntityNotFoundException;

    /**
     *
     * Save a new layout configuration
     *
     * @param layout
     * @return Layout
     * @throws EntityException
     * @since 1.0-SNAPSHOT
     */
    Layout saveLayout(Layout layout) throws EntityException;

    /**
     *
     * Save a new layout configuration
     *
     * @param layout
     * @return Layout
     * @throws EntityException
     * @since 1.0-SNAPSHOT
     */
    Layout updateLayout(Layout layout) throws EntityException;

}
