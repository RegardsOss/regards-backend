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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.domain.Theme;

/**
 *
 * Class IThemeService
 *
 * Interface for Theme service
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RegardsTransactional
public interface IThemeService {

    /**
     *
     * Retreive a Theme by is id.
     *
     * @param pThemeId
     * @return {@link Theme}
     * @since 1.0-SNAPSHOT
     */
    Theme retrieveTheme(Long pThemeId) throws EntityNotFoundException;

    /**
     *
     * Retrieve all themes
     *
     * @param pPageable
     * @return Paged list of {@link Theme}
     * @since 1.0-SNAPSHOT
     */
    Page<Theme> retrieveThemes(Pageable pPageable);

    /**
     *
     * Save a new theme
     *
     * @param pTheme
     *            {@link Theme} to save
     * @return saved {@link Theme}
     * @since 1.0-SNAPSHOT
     */
    Theme saveTheme(Theme pTheme) throws EntityInvalidException;

    /**
     *
     * Update a theme
     *
     * @param pTheme
     *            {@link Theme} to update
     * @return updated {@link Theme}
     * @since 1.0-SNAPSHOT
     */
    Theme updateTheme(Theme pTheme) throws EntityException;

    /**
     *
     * Delete a theme
     *
     * @param pThemeId
     *            Theme id to delete
     *
     * @since 1.0-SNAPSHOT
     */
    void deleteTheme(Long pThemeId) throws EntityNotFoundException;

}
