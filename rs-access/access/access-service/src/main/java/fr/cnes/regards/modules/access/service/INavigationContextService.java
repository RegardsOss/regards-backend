/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.access.service;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.access.domain.NavigationContext;

/**
 * 
 * @author Christophe Mertz
 *
 */
public interface INavigationContextService {

    /**
     * Create a {@link NavigationContext}
     * 
     * @param pNavigationContext
     *            the {@link NavigationContext} to create
     * @return the created {@link NavigationContext}
     * @throws AlreadyExistingException
     *             the {@link NavigationContext} already exists
     */
    NavigationContext create(NavigationContext pNavigationContext) throws AlreadyExistingException;

    /**
     * Update a {@link NavigationContext}.
     * 
     * @param pNavCtxId
     *            the id of the {@link NavigationContext} to load
     * @param pNavigationContext
     *            the {@link NavigationContext} to update
     * @throws EntityNotFoundException
     *             throw if an error occurs
     */
    void update(NavigationContext pNavigationContext) throws EntityNotFoundException;

    /**
     * Delete a {@link NavigationContext}.
     * 
     * @param pNavCtxId
     *            the id of the {@link NavigationContext} to load
     * @throws EntityNotFoundException
     *             throw if an error occurs
     */
    void delete(Long pNavCtxId) throws EntityNotFoundException;

    /**
     * Load a {@link NavigationContext}
     * 
     * @param pNavCtxId
     *            the id of the {@link NavigationContext} to load
     * @return the loaded {@link NavigationContext}
     * @throws EntityNotFoundException
     *             throw if an error occurs
     */
    NavigationContext load(Long pNavCtxId) throws EntityNotFoundException;

    /**
     * Lists all the {@link} {@link NavigationContext}.
     * 
     * @return a list of {@link NavigationContext}
     */
    List<NavigationContext> list();

}
