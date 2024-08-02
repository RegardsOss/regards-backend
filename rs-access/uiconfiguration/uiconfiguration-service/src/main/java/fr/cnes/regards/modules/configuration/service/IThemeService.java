/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.configuration.domain.Theme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Class IThemeService
 * <p>
 * Interface for Theme service
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RegardsTransactional
public interface IThemeService {

    /**
     * Retreive a Theme by is id.
     *
     * @return {@link Theme}
     * @since 1.0-SNAPSHOT
     */
    Theme retrieveTheme(Long themeId) throws EntityNotFoundException;

    /**
     * Retrieve all themes
     *
     * @return Paged list of {@link Theme}
     * @since 1.0-SNAPSHOT
     */
    Page<Theme> retrieveThemes(Pageable pageable);

    /**
     * Save a new theme
     *
     * @param theme {@link Theme} to save
     * @return saved {@link Theme}
     * @since 1.0-SNAPSHOT
     */
    Theme saveTheme(Theme theme);

    /**
     * Update a theme
     *
     * @param theme {@link Theme} to update
     * @return updated {@link Theme}
     * @since 1.0-SNAPSHOT
     */
    Theme updateTheme(Theme theme) throws EntityException;

    /**
     * Delete a theme
     *
     * @param themeId Theme id to delete
     * @since 1.0-SNAPSHOT
     */
    void deleteTheme(Long themeId) throws EntityNotFoundException;

    /**
     * Retrieve all themes
     *
     * @return all themes
     * @since 3.0.0
     */
    List<Theme> retrieveAllThemes();

    /**
     * Retrieve a theme according to its name
     *
     * @param name theme of the theme
     * @return optional theme
     * @since 3.0.0
     */
    Optional<Theme> retrieveByName(String name);
}
