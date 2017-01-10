/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;

/**
 * {@link PluginParameter} repository
 *
 * @author Christophe Mertz
 *
 */
@Repository
public interface IPluginParameterRepository extends CrudRepository<PluginParameter, Long> {

    @Query("from PluginParameter pp join fetch pp.dynamicsValues where id=:id")
    PluginParameter findOneWithDynamicsValues(@Param("id") Long pId);

}
