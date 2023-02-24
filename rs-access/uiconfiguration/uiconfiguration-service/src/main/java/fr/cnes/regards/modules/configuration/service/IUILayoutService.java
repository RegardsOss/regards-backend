/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.configuration.domain.UILayout;

/**
 * Class ILayoutService
 * <p>
 * Service to manage layout configuration entities.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RegardsTransactional
public interface IUILayoutService {

    /**
     * Retrieve an application layout configuration by is applicationId
     *
     * @return Layout
     * @since 1.0-SNAPSHOT
     */
    UILayout retrieveLayout(String applicationId) throws EntityNotFoundException;

    /**
     * Save a new layout configuration
     *
     * @return Layout
     * @since 1.0-SNAPSHOT
     */
    UILayout saveLayout(UILayout UILayout) throws EntityException;

    /**
     * Save a new layout configuration
     *
     * @return Layout
     * @since 1.0-SNAPSHOT
     */
    UILayout updateLayout(UILayout UILayout) throws EntityException;

}
