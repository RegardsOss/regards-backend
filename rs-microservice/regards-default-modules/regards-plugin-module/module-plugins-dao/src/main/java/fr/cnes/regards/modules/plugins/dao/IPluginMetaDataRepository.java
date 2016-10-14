/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.dao;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;

/**
 * {@link PluginMetaData} repository
 * 
 * @author cmertz
 *
 */

@InstanceEntity
public interface IPluginMetaDataRepository extends CrudRepository<PluginMetaData, Long> {

}
