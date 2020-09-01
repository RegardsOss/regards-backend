/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.persistence.Column;
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
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;

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

    protected static final String REQUEST_TYPE_COLUMN = "request_type";

    protected static final String GROUP_ID = "group_id";

    protected static final String COPY = "COPY";

    protected static final String UPDATE = "UPDATE";

    protected static final String NOTIFICATION = "NOTIFICATION";

    protected static final String CREATION = "CREATION";

    protected static final String DELETION = "DELETION";

    @Id
    @SequenceGenerator(name = "featureRequestSequence", initialValue = 1, sequenceName = "seq_feature_request")
    @GeneratedValue(generator = "featureRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(columnDefinition = "jsonb", name = "errors")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> errors;

    @Column(name = GROUP_ID)
    private String groupId;

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

    public Set<String> getErrors() {
        return errors;
    }

    public void setErrors(Set<String> errors) {
        this.errors = errors;
    }

    public void addError(String error) {
        if (errors == null) {
            errors = new HashSet<>();
        }
        errors.add(error);
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
