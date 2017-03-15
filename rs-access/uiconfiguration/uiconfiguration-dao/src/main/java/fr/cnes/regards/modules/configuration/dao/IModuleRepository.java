/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.configuration.domain.Module;

public interface IModuleRepository extends CrudRepository<Module, Long> {

    /**
     *
     * Retrieve modules for the given application id.
     *
     * @param pApplicationId
     * @return {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Page<Module> findByApplicationId(String pApplicationId, Pageable pPageable);

    /**
     *
     * Retrieve modules for the given application id without pagination
     *
     * @param pApplicationId
     * @return
     * @since 1.0-SNAPSHOT
     */
    List<Module> findByApplicationIdAndDefaultDynamicModuleTrue(String pApplicationId);

    /**
     *
     * Retrieve modules for the given application id.
     *
     * @param pApplicationId
     * @return {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Page<Module> findByApplicationIdAndActiveTrue(String pApplicationId, Pageable pPageable);

}
