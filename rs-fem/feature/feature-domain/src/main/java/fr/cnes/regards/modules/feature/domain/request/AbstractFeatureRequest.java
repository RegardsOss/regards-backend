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
package fr.cnes.regards.modules.feature.domain.request;

import java.time.OffsetDateTime;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.springframework.util.Assert;

import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.dto.urn.converter.FeatureUrnConverter;

/**
 * Common request properties
 *
 * @author Marc SORDI
 *
 */
@Entity
@Table(name = "t_feature_request",
        indexes = { @Index(name = "idx_feature_request_id", columnList = AbstractRequest.COLUMN_REQUEST_ID),
                @Index(name = "idx_feature_request_urn", columnList = "urn"),
                @Index(name = "idx_feature_request_type", columnList = AbstractFeatureRequest.REQUEST_TYPE_COLUMN),
                @Index(name = "idx_feature_request_state", columnList = AbstractRequest.COLUMN_STATE),
                @Index(name = "idx_feature_step_registration_priority",
                        columnList = AbstractRequest.COLUMN_STEP + "," + AbstractRequest.COLUMN_REGISTRATION_DATE + ","
                                + AbstractRequest.COLUMN_PRIORITY),
                @Index(name = "idx_feature_request_group_id", columnList = AbstractFeatureRequest.GROUP_ID) })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = AbstractFeatureRequest.REQUEST_TYPE_COLUMN)
public abstract class AbstractFeatureRequest extends AbstractRequest {

    public static final String REQUEST_TYPE_COLUMN = "request_type";

    protected static final String GROUP_ID = "group_id";

    @Id
    @SequenceGenerator(name = "featureRequestSequence", initialValue = 1, sequenceName = "seq_feature_request",
            allocationSize = 1000)
    @GeneratedValue(generator = "featureRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = GROUP_ID)
    protected String groupId;

    /**
     * Information Package ID for REST request
     */
    @Column(name = "urn", nullable = false, length = FeatureUniformResourceName.MAX_SIZE)
    @Convert(converter = FeatureUrnConverter.class)
    protected FeatureUniformResourceName urn;

    @SuppressWarnings("unchecked")
    protected <T extends AbstractFeatureRequest> T with(String requestId, String requestOwner,
            OffsetDateTime requestDate, RequestState state, FeatureRequestStep step, PriorityLevel priority,
            Set<String> errors) {
        Assert.notNull(requestId, "Request id is required");
        Assert.notNull(requestDate, "Request date is required");
        Assert.notNull(state, "Request state is required");
        Assert.notNull(step, "Request step is required");
        Assert.notNull(priority, "Request priority is required");
        super.with(requestId, requestOwner, requestDate, priority, state, step);
        this.errors = errors;
        return (T) this;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUrn(FeatureUniformResourceName urn) {
        this.urn = urn;
    }

    public FeatureUniformResourceName getUrn() {
        return urn;
    }

    public abstract <U> U accept(IAbstractFeatureRequestVisitor<U> visitor);

    public static FeatureRequestDTO toDTO(AbstractFeatureRequest request) {
        FeatureRequestDTO dto = AbstractRequest.toDTO(request);
        dto.setUrn(request.getUrn());
        return dto;
    }
}