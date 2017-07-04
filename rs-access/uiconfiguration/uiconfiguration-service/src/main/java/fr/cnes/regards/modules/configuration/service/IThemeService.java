/*
 * LICENSE_PLACEHOLDER
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
