/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.modules.plugins.domain.PluginDynamicValue;

/**
 * {@link PluginDynamicValue} repository
 * 
 * @author Christophe Mertz
 *
 */
@Repository
public interface IPluginDynamicValueRepository extends CrudRepository<PluginDynamicValue, Long> {

}
