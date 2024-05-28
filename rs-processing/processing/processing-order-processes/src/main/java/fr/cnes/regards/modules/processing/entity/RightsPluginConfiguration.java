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
package fr.cnes.regards.modules.processing.entity;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * This class decorates a PluginConfiguration, corresponding to a Process plugin,
 * with associated access rights for the corresponding process.
 * <p>
 * It allows to determine that a given process is usable by a given user role,
 * and for a given list of datasets.
 *
 * @author gandrieu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_rights_plugin_configuration")
@SequenceGenerator(name = "pluginRightsConfSequence", initialValue = 1, sequenceName = "seq_plugin_rights_conf")
public class RightsPluginConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pluginRightsConfSequence")
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "plugin_configuration_id", foreignKey = @ForeignKey(name = "fk_rights_plugin_configuration"))
    private PluginConfiguration pluginConfiguration;

    @Column(name = "process_business_id")
    private UUID processBusinessId;

    @Column(name = "user_role", columnDefinition = "text")
    private String role;

    @Column(name = "datasets", columnDefinition = "varchar(128)[]")
    @Type(StringArrayType.class)
    private String[] datasets;

    @Column(name = "is_linked_to_all_datasets")
    private boolean linkedToAllDatasets;

    public List<String> getDatasets() {
        return Arrays.asList(datasets);
    }

    public boolean isLinkedToAllDatasets() {
        return linkedToAllDatasets;
    }

    public static ProcessPluginConfigurationRightsDTO toDto(RightsPluginConfiguration rights) {
        return new ProcessPluginConfigurationRightsDTO(rights.getPluginConfiguration(),
                                                       new ProcessPluginConfigurationRightsDTO.Rights(rights.getRole(),
                                                                                                      io.vavr.collection.List.ofAll(
                                                                                                          rights.getDatasets()),
                                                                                                      rights.isLinkedToAllDatasets()));
    }

    public static RightsPluginConfiguration fromDto(ProcessPluginConfigurationRightsDTO dto) {
        return new RightsPluginConfiguration(null,
                                             dto.getPluginConfiguration(),
                                             UUID.fromString(dto.getPluginConfiguration().getBusinessId()),
                                             dto.getRights().getRole(),
                                             dto.getRights().getDatasets().toJavaArray(String[]::new),
                                             dto.getRights().isLinkedToAllDatasets());
    }
}
