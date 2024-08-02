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

import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureDisseminationInfoDto;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "t_feature",
       indexes = { @Index(name = "idx_feature_last_update", columnList = "last_update"),
                   @Index(name = "idx_feature_urn", columnList = "urn"),
                   @Index(name = "idx_feature_session", columnList = "session_owner,session_name"),
                   @Index(name = "idx_feature_provider_id", columnList = "provider_id") },
       uniqueConstraints = { @UniqueConstraint(name = "uk_feature_urn", columnNames = { "urn" }) })
public class FeatureEntity extends AbstractFeatureEntity {

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", foreignKey = @ForeignKey(name = "fk_feature_dissemination_info_feature_id"))
    private Set<FeatureDisseminationInfo> disseminationsInfo = new HashSet<>();

    public static FeatureEntity build(String sessionOwner,
                                      String session,
                                      Feature feature,
                                      FeatureUniformResourceName previousVersionUrn,
                                      String model) {
        FeatureEntity featureEntity = new FeatureEntity();
        featureEntity.setSessionOwner(sessionOwner);
        featureEntity.setSession(session);
        featureEntity.setFeature(feature);
        featureEntity.setLastUpdate(OffsetDateTime.now());
        featureEntity.setProviderId(feature.getId());
        featureEntity.setUrn(feature.getUrn());
        featureEntity.setVersion(feature.getUrn().getVersion());
        featureEntity.setPreviousVersionUrn(previousVersionUrn);
        featureEntity.setCreationDate(featureEntity.getLastUpdate());
        featureEntity.setModel(model);
        featureEntity.setDisseminationPending(false);
        return featureEntity;
    }

    public FeatureEntityDto toDto(boolean addFeatureContent) {
        FeatureEntityDto dto = new FeatureEntityDto();
        dto.setSession(this.getSession());
        dto.setSource(this.getSessionOwner());
        dto.setProviderId(this.getProviderId());
        dto.setVersion(this.getVersion());
        dto.setLastUpdate(this.getLastUpdate());
        dto.setUrn(this.getUrn());
        dto.setId(this.getId());
        dto.setDisseminationPending(this.isDisseminationPending());
        dto.setDisseminationsInfo(this.getDisseminationsInfo()
                                      .stream()
                                      .map(featureDisseminationInfo -> new FeatureDisseminationInfoDto(
                                          featureDisseminationInfo.getLabel(),
                                          featureDisseminationInfo.getRequestDate(),
                                          featureDisseminationInfo.getAckDate(),
                                          featureDisseminationInfo.isBlocking()))
                                      .collect(Collectors.toSet()));
        if (addFeatureContent) {
            dto.setFeature(this.getFeature());
        }
        return dto;
    }

    public Set<FeatureDisseminationInfo> getDisseminationsInfo() {
        return disseminationsInfo;
    }

    public void setDisseminationsInfo(Set<FeatureDisseminationInfo> disseminationsInfo) {
        this.disseminationsInfo = disseminationsInfo;
    }

    public void updateDisseminationPending() {
        setDisseminationPending(this.disseminationsInfo.stream()
                                                       .anyMatch(disseminationInfo -> disseminationInfo.getAckDate()
                                                                                      == null));
    }

}
