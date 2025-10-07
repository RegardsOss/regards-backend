/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.domain;

import com.google.gson.JsonParser;
import fr.cnes.regards.modules.feature.dto.FeatureEntityRawDto;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entity representing a feature with the dissemination info and with the feature field serialized as a JSON String.
 *
 * @author Thibaud Michaudel
 * @see FeatureEntity for the version with the dissemination info
 * @see FeatureSimpleEntity for the version without dissemination info
 * @see FeatureSimpleRawEntity for the version without dissemination info and with the feature field as a JSON String
 */
@Entity
@Table(name = "t_feature",
       indexes = { @Index(name = "idx_feature_last_update", columnList = "last_update"),
                   @Index(name = "idx_feature_urn", columnList = "urn"),
                   @Index(name = "idx_feature_session", columnList = "session_owner,session_name"),
                   @Index(name = "idx_feature_provider_id", columnList = "provider_id") },
       uniqueConstraints = { @UniqueConstraint(name = "uk_feature_urn", columnNames = { "urn" }) })
public class FeatureRawEntity extends AbstractFeatureRawEntity {

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", foreignKey = @ForeignKey(name = "fk_feature_dissemination_info_feature_id"))
    private Set<FeatureDisseminationInfo> disseminationsInfo = new HashSet<>();

    public Set<FeatureDisseminationInfo> getDisseminationsInfo() {
        return disseminationsInfo;
    }

    public FeatureEntityRawDto toDto() {
        return new FeatureEntityRawDto(getId(),
                                       getUrn(),
                                       getSessionOwner(),
                                       getSession(),
                                       getProviderId(),
                                       getVersion(),
                                       getLastUpdate(),
                                       JsonParser.parseString(this.getFeature()).getAsJsonObject(),
                                       isDisseminationPending(),
                                       getDisseminationsInfo().stream()
                                                              .map(FeatureDisseminationInfo::toDto)
                                                              .collect(Collectors.toSet()));
    }
}
