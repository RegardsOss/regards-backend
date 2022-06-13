/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.dto;

import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.hibernate.annotations.Type;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * {@link FeatureEntityDto} dto
 *
 * @author Kevin Marchois
 */
public class FeatureEntityDto {

    @NotNull
    private Long id;

    @NotNull
    private FeatureUniformResourceName urn;

    @NotNull
    private String source;

    @NotNull
    private String session;

    @NotNull
    private String providerId;

    @NotNull
    private Integer version;

    @NotNull
    private OffsetDateTime lastUpdate;

    @Type(type = "jsonb")
    @Valid
    private Feature feature;

    private boolean disseminationPending;

    private Set<FeatureDisseminationInfoDto> disseminationsInfo = new HashSet<>();

    public static FeatureEntityDto build(String source,
                                         String session,
                                         Feature feature,
                                         String providerId,
                                         Integer version,
                                         OffsetDateTime lastUpdate) {
        FeatureEntityDto featureEntity = new FeatureEntityDto();
        featureEntity.setSource(source);
        featureEntity.setSession(session);
        featureEntity.setFeature(feature);
        featureEntity.setProviderId(providerId);
        featureEntity.setVersion(version);
        featureEntity.setLastUpdate(lastUpdate);
        return featureEntity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public FeatureUniformResourceName getUrn() {
        return urn;
    }

    public void setUrn(FeatureUniformResourceName urn) {
        this.urn = urn;
    }

    public boolean isDisseminationPending() {
        return disseminationPending;
    }

    public void setDisseminationPending(boolean disseminationPending) {
        this.disseminationPending = disseminationPending;
    }

    public Set<FeatureDisseminationInfoDto> getDisseminationsInfo() {
        return disseminationsInfo;
    }

    public void setDisseminationsInfo(Set<FeatureDisseminationInfoDto> disseminationsInfo) {
        this.disseminationsInfo = disseminationsInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        FeatureEntityDto that = (FeatureEntityDto) o;

        if (disseminationPending != that.disseminationPending)
            return false;
        if (!id.equals(that.id))
            return false;
        if (!urn.equals(that.urn))
            return false;
        if (!source.equals(that.source))
            return false;
        if (!session.equals(that.session))
            return false;
        if (!providerId.equals(that.providerId))
            return false;
        if (!version.equals(that.version))
            return false;
        if (!Objects.equals(feature, that.feature))
            return false;
        return Objects.equals(disseminationsInfo, that.disseminationsInfo);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + urn.hashCode();
        result = 31 * result + source.hashCode();
        result = 31 * result + session.hashCode();
        result = 31 * result + providerId.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + (feature != null ? feature.hashCode() : 0);
        result = 31 * result + (disseminationPending ? 1 : 0);
        result = 31 * result + (disseminationsInfo != null ? disseminationsInfo.hashCode() : 0);
        return result;
    }
}
