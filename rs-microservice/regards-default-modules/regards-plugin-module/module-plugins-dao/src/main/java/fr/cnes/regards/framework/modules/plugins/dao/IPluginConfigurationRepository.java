/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

/**
 * {@link PluginConfiguration} repository
 * 
 * @author Christophe Mertz
 *
 */
@Repository
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

    @Query("from PluginConfiguration pc join fetch pc.parameters where parent_conf_id=:id")
    PluginConfiguration findOneWithPluginParameter(@Param("id") Long pId);

}
