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
package fr.cnes.regards.modules.feature.domain.request.dissemination;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.dto.urn.converter.FeatureUrnConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * A request to update the {@link FeatureEntity} acknowledge and/or blocking on one of its recipient
 *
 * @author Léo Mieulet
 */
@Entity
@Table(name = "t_feature_update_dissemination")
public class FeatureUpdateDisseminationRequest {

    @Id
    @SequenceGenerator(name = "featureUpdateDisseminationSequence",
                       initialValue = 1,
                       sequenceName = "seq_feature_update_dissemination")
    @GeneratedValue(generator = "featureUpdateDisseminationSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "feature_urn", nullable = false, length = FeatureUniformResourceName.MAX_SIZE)
    @Convert(converter = FeatureUrnConverter.class)
    private FeatureUniformResourceName urn;

    /**
     * The name of the recipient
     */
    @Column(name = "recipient_label", length = 128, nullable = false)
    private String recipientLabel;

    /**
     * Real Dissemination date of the product to current recipient
     */
    @Column(name = "creation_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    @NotNull
    private OffsetDateTime creationDate;

    @Column(name = "update_type", nullable = false)
    private FeatureUpdateDisseminationInfoType updateType;

    /**
     * When false, no acknowledge message will be received
     * Only defined when request type is {@link FeatureUpdateDisseminationInfoType#PUT}
     */
    @Column(name = "ack_required")
    private Boolean ackRequired;

    @Column(name = "blocking_required")
    private boolean blockingRequired;

    public FeatureUpdateDisseminationRequest() {
    }

    public FeatureUpdateDisseminationRequest(FeatureUniformResourceName urn,
                                             String recipientLabel,
                                             FeatureUpdateDisseminationInfoType updateType,
                                             OffsetDateTime disseminationDate) {
        this.urn = urn;
        this.recipientLabel = recipientLabel;
        this.updateType = updateType;
        this.creationDate = disseminationDate;
    }

    public FeatureUpdateDisseminationRequest(FeatureUniformResourceName urn,
                                             String recipientLabel,
                                             FeatureUpdateDisseminationInfoType updateType,
                                             OffsetDateTime disseminationDate,
                                             boolean ackRequired,
                                             boolean blockingRequired) {
        this(urn, recipientLabel, updateType, disseminationDate);

        this.ackRequired = ackRequired;
        this.blockingRequired = blockingRequired;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FeatureUniformResourceName getUrn() {
        return urn;
    }

    public void setUrn(FeatureUniformResourceName urn) {
        this.urn = urn;
    }

    public String getRecipientLabel() {
        return recipientLabel;
    }

    public void setRecipientLabel(String recipientLabel) {
        this.recipientLabel = recipientLabel;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public FeatureUpdateDisseminationInfoType getUpdateType() {
        return updateType;
    }

    public void setUpdateType(FeatureUpdateDisseminationInfoType updateType) {
        this.updateType = updateType;
    }

    public Boolean getAckRequired() {
        return ackRequired;
    }

    public void setAckRequired(Boolean ackRequired) {
        this.ackRequired = ackRequired;
    }

    public boolean isBlockingRequired() {
        return blockingRequired;
    }

    public void setBlockingRequired(boolean blocking) {
        this.blockingRequired = blocking;
    }
}
