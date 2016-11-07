/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.dao;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.plugins.domain.PluginDynamicValue;

/**
 * {@link PluginDynamicValue} repository
 * 
 * @author Christophe Mertz
 *
 */
public interface IPluginDynamicValueRepository extends CrudRepository<PluginDynamicValue, Long> {

}
