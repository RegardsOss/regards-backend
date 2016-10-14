/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;

/**
 * {@link PluginConfiguration} repository
 * 
 * @author cmertz
 *
 */

@InstanceEntity
public interface IPluginConfigurationRepository extends CrudRepository<PluginConfiguration, Long> {

    /**
     *
     * Find a {@link List} of {@link PluginConfiguration} for a plugin
     *
     * @param pPluginId
     *            the plugin identifier
     * @return a {@link List} of {@link PluginConfiguration}
     */
    List<PluginConfiguration> findByPluginIdOrderByPriorityOrderDesc(String pPluginId);
}
