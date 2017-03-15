/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.domain.Module;

/**
 *
 * Class IModuleService
 *
 * Interface for Module service
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Transactional
public interface IModuleService {

    /**
     *
     * Retreive a module by is id.
     *
     * @param pModuleId
     * @return {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Module retrieveModule(Long pModuleId) throws EntityNotFoundException;

    /**
     *
     * Retrieve all modules for the given application Id
     *
     * @param pApplicationId
     * @return Paged list of {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Page<Module> retrieveModules(String pApplicationId, Pageable pPageable);

    /**
     *
     * Retrieve all active modules for the given application Id
     *
     * @param pApplicationId
     * @return Paged list of {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Page<Module> retrieveActiveModules(String pApplicationId, Pageable pPageable);

    /**
     *
     * Save a new module
     *
     * @param pModule
     *            {@link Module} to save
     * @return saved {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Module saveModule(Module pModule) throws EntityInvalidException;

    /**
     *
     * Update a module
     *
     * @param pModule
     *            {@link Module} to update
     * @return updated {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Module updateModule(Module pModule) throws EntityNotFoundException, EntityInvalidException;

    /**
     *
     * Delete a module
     *
     * @param pModuleId
     *            Module id to delete
     *
     * @since 1.0-SNAPSHOT
     */
    void deleteModule(Long pModuleId) throws EntityNotFoundException;

}
