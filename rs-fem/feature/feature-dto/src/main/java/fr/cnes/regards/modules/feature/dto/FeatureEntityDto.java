/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

    public static FeatureEntityDto build(String source, String session, Feature feature,
            FeatureUniformResourceName previousVersionUrn, String model, String providerId, Integer version,
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

}
