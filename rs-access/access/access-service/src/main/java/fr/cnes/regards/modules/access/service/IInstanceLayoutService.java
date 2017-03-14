/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.service;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.access.domain.instance.InstanceLayout;

/**
 *
 * Class IInstanceLayoutService
 *
 * Service to manage instance layout configurations entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@InstanceTransactional
public interface IInstanceLayoutService {

    /**
     *
     * Retrieve an application layout configuration by is applicationId
     *
     * @param applicationId
     * @return Layout
     * @since 1.0-SNAPSHOT
     */
    InstanceLayout retrieveLayout(String pApplicationId) throws EntityNotFoundException;

    /**
     *
     * Save a new layout configuration
     *
     * @param pLayout
     * @return Layout
     * @since 1.0-SNAPSHOT
     */
    InstanceLayout saveLayout(InstanceLayout pLayout) throws EntityAlreadyExistsException;

    /**
     *
     * Save a new layout configuration
     *
     * @param pLayout
     * @return Layout
     * @since 1.0-SNAPSHOT
     */
    InstanceLayout updateLayout(InstanceLayout pLayout) throws EntityNotFoundException, EntityInvalidException;

}
