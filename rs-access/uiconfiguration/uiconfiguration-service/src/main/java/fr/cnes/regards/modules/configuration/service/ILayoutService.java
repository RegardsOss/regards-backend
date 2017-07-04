/*
 * LICENSE_PLACEHOLDER
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
     * @param pApplicationId
     * @return Layout
     * @since 1.0-SNAPSHOT
     */
    Layout retrieveLayout(String pApplicationId) throws EntityNotFoundException;

    /**
     *
     * Save a new layout configuration
     *
     * @param pLayout
     * @return Layout
     * @since 1.0-SNAPSHOT
     */
    Layout saveLayout(Layout pLayout) throws EntityException;

    /**
     *
     * Save a new layout configuration
     *
     * @param pLayout
     * @return Layout
     * @since 1.0-SNAPSHOT
     */
    Layout updateLayout(Layout pLayout) throws EntityException;

}
