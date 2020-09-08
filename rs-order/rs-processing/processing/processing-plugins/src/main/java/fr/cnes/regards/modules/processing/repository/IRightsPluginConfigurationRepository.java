package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import io.vavr.control.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IRightsPluginConfigurationRepository extends JpaRepository<RightsPluginConfiguration, Long> {

    Option<RightsPluginConfiguration> findByPluginConfiguration(PluginConfiguration pluginConfigurationId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = " UPDATE t_rights_plugin_configuration AS rpc "
              + " SET datasets = "
              + "     CASE WHEN rpc.process_business_id IN (:processBusinessIds) THEN "
              + "         CASE WHEN array_position(rpc.datasets, :dataset) IS NULL "
              + "         THEN array_append(rpc.datasets, :dataset) "
              + "         ELSE rpc.datasets "
              + "         END "
              + "     ELSE "
              + "         CASE WHEN array_position(rpc.datasets, :dataset) IS NULL "
              + "         THEN rpc.datasets "
              + "         ELSE array_remove(rpc.datasets, :dataset) "
              + "         END "
              + "     END "
        ,
        nativeQuery = true
    )
    void updateAllAddDatasetOnlyForIds(
            @Param("processBusinessIds") List<UUID> processBusinessIds,
            @Param("dataset") String dataset
    );

    @Query(
        value = " SELECT sub.id,"
              + "        sub.plugin_configuration_id, "
              + "        sub.process_business_id, "
              + "        sub.tenant, "
              + "        sub.user_role, "
              + "        sub.datasets "
              + " FROM ( "
              + "         SELECT rpc.*, unnest(rpc.datasets) AS x "
              + "         FROM t_rights_plugin_configuration AS rpc "
              + " ) AS sub "
              + " WHERE sub.x = CAST(:dataset AS TEXT) ",
            nativeQuery = true
    )
    List<RightsPluginConfiguration> findByReferencedDataset(
            @Param("dataset") String dataset
    );

}
