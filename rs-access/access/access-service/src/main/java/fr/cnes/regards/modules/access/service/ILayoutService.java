/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.access.domain.project.Layout;

/**
 *
 * Class ILayoutService
 *
 * Service to manage layout configuration entities.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@MultitenantTransactional
public interface ILayoutService {

    /**
     *
     * Retrieve an application layout configuration by is applicationId
     *
     * @param applicationId
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
    Layout saveLayout(Layout pLayout) throws EntityAlreadyExistsException;

    /**
     *
     * Save a new layout configuration
     *
     * @param pLayout
     * @return Layout
     * @since 1.0-SNAPSHOT
     */
    Layout updateLayout(Layout pLayout) throws EntityNotFoundException, EntityInvalidException;

    /**
     *
     * Initialize default layouts for given tenant.
     *
     * @param pTenant
     * @since 1.0-SNAPSHOT
     */
    void initProjectLayout(String pTenant);

}
