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

/**
 * {@link FeatureEntityDto} dto
 *
 * @author Kevin Marchois
 */
public class FeatureEntityDto {

    @NotNull
    private String source;

    @NotNull
    private String session;

    @NotNull
    @Type(type = "jsonb")
    @Valid
    private Feature feature;

    public static FeatureEntityDto build(String sessionOwner, String session, Feature feature,
            FeatureUniformResourceName previousVersionUrn, String model) {
        FeatureEntityDto featureEntity = new FeatureEntityDto();
        featureEntity.setSource(sessionOwner);
        featureEntity.setSession(session);
        featureEntity.setFeature(feature);
        return featureEntity;
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

}
