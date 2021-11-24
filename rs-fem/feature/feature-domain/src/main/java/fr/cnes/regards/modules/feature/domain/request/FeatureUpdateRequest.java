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
package fr.cnes.regards.modules.feature.domain.request;

import java.time.OffsetDateTime;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;

/**
 * @author Marc SORDI
 * @author Sébastien Binda
 */
@Entity
@DiscriminatorValue(FeatureRequestTypeEnum.UPDATE_DISCRIMINENT)
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class FeatureUpdateRequest extends AbstractFeatureRequest {

    @Column(name = "provider_id", nullable = false)
    @NotBlank(message = "Provider id is required")
    private String providerId;

    @Column(columnDefinition = "jsonb", name = "feature", nullable = false)
    @Type(type = "jsonb")
    private Feature feature;

    @Embedded
    private FeatureStorageMedataEntity metadata;

    /**
     * Should be null until it reaches {@link FeatureRequestStep#LOCAL_TO_BE_NOTIFIED}
     */
    @Column(columnDefinition = "jsonb", name = "to_notify", nullable = true)
    @Type(type = "jsonb")
    private Feature toNotify;

    @Column(name="sessionToNotify", length=255)
    private String sessionToNotify;

    @Column(name="sourceToNotify", length=255)
    private String sourceToNotify;

    public static FeatureUpdateRequest build(String requestId, String requestOwner, OffsetDateTime requestDate,
            RequestState state, Set<String> errors, Feature feature, PriorityLevel priority, FeatureRequestStep step) {
        Assert.notNull(feature, "Feature is required");
        FeatureUpdateRequest request = new FeatureUpdateRequest();
        request.with(requestId, requestOwner, requestDate, state, step, priority, errors);
        request.setProviderId(feature.getId());
        request.setUrn(feature.getUrn());
        request.setFeature(feature);
        return request;
    }

    @Override
    public FeatureUniformResourceName getUrn() {
        return urn;
    }

    @Override
    public <U> U accept(IAbstractFeatureRequestVisitor<U> visitor) {
        return visitor.visitUpdateRequest(this);
    }

    @Override
    public void setUrn(FeatureUniformResourceName urn) {
        this.urn = urn;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Feature getFeature() {
        return this.feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public Feature getToNotify() {
        return toNotify;
    }

    public void setToNotify(Feature toNotify) {
        this.toNotify = toNotify;
    }

    public String getSessionToNotify() {
        return sessionToNotify;
    }

    public void setSessionToNotify(String sessionToNotify) {
        this.sessionToNotify = sessionToNotify;
    }

    public String getSourceToNotify() {
        return sourceToNotify;
    }

    public void setSourceToNotify(String sourceToNotify) {
        this.sourceToNotify = sourceToNotify;
    }

    public FeatureStorageMedataEntity getMetadata() {
        return metadata;
    }

    public void setMetadata(FeatureStorageMedataEntity metadata) {
        this.metadata = metadata;
    }
}
