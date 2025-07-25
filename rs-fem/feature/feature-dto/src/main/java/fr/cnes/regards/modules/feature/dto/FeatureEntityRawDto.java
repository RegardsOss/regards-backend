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
package fr.cnes.regards.modules.feature.dto;

import com.google.gson.JsonObject;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * {@link FeatureEntityDto} with the {@link Feature} serialized as a JSON object.
 *
 * @author Thibaud Michaudel
 */
public class FeatureEntityRawDto {

    @NotNull
    private final Long id;

    @NotNull
    private final FeatureUniformResourceName urn;

    @NotNull
    private final String source;

    @NotNull
    private final String session;

    @NotNull
    private final String providerId;

    @NotNull
    private final Integer version;

    @NotNull
    private final OffsetDateTime lastUpdate;

    private final JsonObject feature;

    private final boolean disseminationPending;

    public FeatureEntityRawDto(Long id,
                               FeatureUniformResourceName urn,
                               String source,
                               String session,
                               String providerId,
                               Integer version,
                               OffsetDateTime lastUpdate,
                               @Nullable JsonObject feature,
                               boolean disseminationPending) {
        this.id = id;
        this.urn = urn;
        this.source = source;
        this.session = session;
        this.providerId = providerId;
        this.version = version;
        this.lastUpdate = lastUpdate;
        this.feature = feature;
        this.disseminationPending = disseminationPending;
    }

    public Long getId() {
        return id;
    }

    public JsonObject getFeature() {
        return feature;
    }

    public String getSource() {
        return source;
    }

    public String getSession() {
        return session;
    }

    public String getProviderId() {
        return providerId;
    }

    public Integer getVersion() {
        return version;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public FeatureUniformResourceName getUrn() {
        return urn;
    }

    public boolean isDisseminationPending() {
        return disseminationPending;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeatureEntityRawDto that = (FeatureEntityRawDto) o;

        if (disseminationPending != that.disseminationPending) {
            return false;
        }
        if (!id.equals(that.id)) {
            return false;
        }
        if (!urn.equals(that.urn)) {
            return false;
        }
        if (!source.equals(that.source)) {
            return false;
        }
        if (!session.equals(that.session)) {
            return false;
        }
        if (!providerId.equals(that.providerId)) {
            return false;
        }
        if (!version.equals(that.version)) {
            return false;
        }
        return Objects.equals(feature, that.feature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, urn, source, session, providerId, version, lastUpdate, feature, disseminationPending);
    }

    @Override
    public String toString() {
        return "FeatureEntityRawDto{"
               + "id="
               + id
               + ", urn="
               + urn
               + ", source='"
               + source
               + '\''
               + ", session='"
               + session
               + '\''
               + ", providerId='"
               + providerId
               + '\''
               + ", version="
               + version
               + ", lastUpdate="
               + lastUpdate
               + ", feature="
               + feature
               + ", disseminationPending="
               + disseminationPending
               + '}';
    }
}
