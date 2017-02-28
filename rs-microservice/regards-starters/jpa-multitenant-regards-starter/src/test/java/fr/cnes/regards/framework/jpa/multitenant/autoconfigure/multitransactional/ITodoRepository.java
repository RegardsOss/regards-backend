/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.multitransactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Marc Sordi
 *
 */
@Repository
public interface ITodoRepository extends CrudRepository<Todo, Long> {

}
