/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.access.service;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
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
     * @throws ModuleException
     *             the {@link NavigationContext} already exists
     */
    NavigationContext create(NavigationContext pNavigationContext) throws ModuleException;

    /**
     * Update a {@link NavigationContext}.
     * 
     * @param pNavigationContext
     *            the {@link NavigationContext} to update
     * @return The {@link NavigationContext} updated
     * @throws ModuleEntityNotFoundException
     *             throw if an error occurs
     */
    NavigationContext update(NavigationContext pNavigationContext) throws ModuleEntityNotFoundException;

    /**
     * Delete a {@link NavigationContext}.
     * 
     * @param pNavCtxId
     *            the id of the {@link NavigationContext} to load
     * @throws ModuleEntityNotFoundException
     *             throw if an error occurs
     */
    void delete(Long pNavCtxId) throws ModuleEntityNotFoundException;

    /**
     * Load a {@link NavigationContext}
     * 
     * @param pNavCtxId
     *            the id of the {@link NavigationContext} to load
     * @return the loaded {@link NavigationContext}
     * @throws ModuleEntityNotFoundException
     *             throw if an error occurs
     */
    NavigationContext load(Long pNavCtxId) throws ModuleEntityNotFoundException;

    /**
     * Lists all the {@link} {@link NavigationContext}.
     * 
     * @return a list of {@link NavigationContext}
     */
    List<NavigationContext> list();

}
