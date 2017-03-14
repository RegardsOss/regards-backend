/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.dao.instance;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.access.domain.instance.InstanceLayout;

/**
 *
 * Class IInstanceLayoutRepository
 *
 * JPA Repository to manage IHM instance layouts entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@InstanceEntity
public interface IInstanceLayoutRepository extends CrudRepository<InstanceLayout, Long> {

    /**
     *
     * Retrieve a layout by is application id
     *
     * @param pApplicationId
     * @return {@link InstanceLayout}
     * @since 1.0-SNAPSHOT
     */
    Optional<InstanceLayout> findByApplicationId(String pApplicationId);

}
