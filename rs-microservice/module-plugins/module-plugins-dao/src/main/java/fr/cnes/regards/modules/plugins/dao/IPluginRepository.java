/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.dao;


import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;

public interface IPluginRepository extends CrudRepository<PluginConfiguration, Long> {

}
