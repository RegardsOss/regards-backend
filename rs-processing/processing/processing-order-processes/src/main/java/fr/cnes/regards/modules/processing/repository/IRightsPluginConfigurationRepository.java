/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
*/
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

/**
 * This interface is a {@link JpaRepository} for {@link RightsPluginConfiguration}.
 *
 * @author gandrieu
 */
@Repository
public interface IRightsPluginConfigurationRepository extends JpaRepository<RightsPluginConfiguration, Long> {

    Option<RightsPluginConfiguration> findByPluginConfigurationBusinessId(String processBusinessId);

    Option<RightsPluginConfiguration> findByPluginConfiguration(PluginConfiguration pluginConfigurationId);

    @Query(
            value = " SELECT "
                    + " DISTINCT ON (sub.id) "
                    + "        sub.id,"
                    + "        sub.plugin_configuration_id, "
                    + "        sub.process_business_id, "
                    + "        sub.user_role, "
                    + "        sub.datasets, "
                    + "        sub.is_linked_to_all_datasets "
                    + " FROM ( "
                    + "         SELECT rpc.*, unnest(array_append(rpc.datasets, '')) AS x "
                    + "         FROM t_rights_plugin_configuration AS rpc "
                    + " ) AS sub "
                    + " WHERE sub.x = CAST(:dataset AS TEXT)"
                    + "    OR sub.is_linked_to_all_datasets = TRUE"
                    + " ORDER BY sub.id ",
            nativeQuery = true
    )
    List<RightsPluginConfiguration> findByReferencedDataset(
            @Param("dataset") String dataset
    );

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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = " UPDATE t_rights_plugin_configuration AS rpc "
                    + " SET user_role = :userRole "
                    + " WHERE rpc.process_business_id = :processBusinessId "
            ,
            nativeQuery = true
    )
    void updateRoleToForProcessBusinessId(@Param("userRole") String userRole, @Param("processBusinessId") UUID processBusinessId);
}
