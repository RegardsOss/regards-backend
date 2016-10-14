/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.dao;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;

/**
 * {@link PluginParameter} repository
 * 
 * @author cmertz
 *
 */

@InstanceEntity
public interface IPluginParameterRepository extends CrudRepository<PluginParameter, Long> {

}
