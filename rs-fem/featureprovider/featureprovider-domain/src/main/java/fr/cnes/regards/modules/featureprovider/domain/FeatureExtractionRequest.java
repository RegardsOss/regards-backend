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
package fr.cnes.regards.modules.featureprovider.domain;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;

import com.google.gson.JsonObject;

import fr.cnes.regards.modules.feature.domain.request.AbstractRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationMetadataEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;

/**
 * Contain a reference to to file plus a plugin business id
 * @author Kevin Marchois
 *
 */
@Entity
@Table(name = "t_feature_extraction_request",
        indexes = { @Index(name = "idx_feature_extraction_request_step", columnList = AbstractRequest.COLUMN_STEP) },
        uniqueConstraints = { @UniqueConstraint(name = "uk_feature_extraction_request_id",
                columnNames = { AbstractRequest.COLUMN_REQUEST_ID }) })
public class FeatureExtractionRequest extends AbstractRequest {

    public static final String REQUEST_TYPE = "EXTRACTION";

    @Id
    @SequenceGenerator(name = "featureExtractionRequestSequence", initialValue = 1,
            sequenceName = "seq_feature_extraction_request")
    @GeneratedValue(generator = "featureExtractionRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Embedded
    private FeatureCreationMetadataEntity metadata;

    @Column(name = "extraction_factory", nullable = false)
    private String factory;

    @Column(columnDefinition = "jsonb", name = "extraction_parameters", nullable = false)
    @Type(type = "jsonb")
    private JsonObject parameters;

    public static FeatureExtractionRequest build(String requestId, String requestOwner, OffsetDateTime requestDate,
            RequestState state, FeatureCreationMetadataEntity metadata, FeatureRequestStep step, PriorityLevel priority,
            JsonObject parameters, String factory) {
        FeatureExtractionRequest request = new FeatureExtractionRequest();
        request.with(requestId, requestOwner, requestDate, priority, state, step);
        request.setMetadata(metadata);
        request.setFactory(factory);
        request.setParameters(parameters);
        request.setRegistrationDate(OffsetDateTime.now());
        return request;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FeatureCreationMetadataEntity getMetadata() {
        return metadata;
    }

    public void setMetadata(FeatureCreationMetadataEntity metadata) {
        this.metadata = metadata;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public JsonObject getParameters() {
        return parameters;
    }

    public void setParameters(JsonObject parameters) {
        this.parameters = parameters;
    }

}
