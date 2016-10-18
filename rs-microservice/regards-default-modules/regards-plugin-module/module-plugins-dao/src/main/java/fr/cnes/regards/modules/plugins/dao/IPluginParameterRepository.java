/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.dao;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.plugins.domain.PluginParameter;

/**
 * {@link PluginParameter} repository
 * 
 * @author cmertz
 *
 */

public interface IPluginParameterRepository extends CrudRepository<PluginParameter, Long> {

}
