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
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.Type;

import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;

/**
 * @author Kevin Marchois
 *
 */
@Entity
@DiscriminatorValue(FeatureRequestTypeEnum.DELETION_DISCRIMINENT)
public class FeatureDeletionRequest extends AbstractFeatureRequest {

    @Id
    @SequenceGenerator(name = "featureDeleteRequestSequence", initialValue = 1,
            sequenceName = "seq_feature_deletion_request")
    @GeneratedValue(generator = "featureDeleteRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Should be null until it reaches {@link FeatureRequestStep#LOCAL_TO_BE_NOTIFIED}
     */
    @Column(columnDefinition = "jsonb", name = "to_notify", nullable = true)
    @Type(type = "jsonb")
    private Feature toNotify;

    /**
     * This is used to notify user. Should only be setted by deletion process
     */
    @Column(name = "already_deleted")
    private boolean alreadyDeleted;

    public static FeatureDeletionRequest build(String requestId, String requestOwner, OffsetDateTime requestDate,
            RequestState state, Set<String> errors, FeatureRequestStep step, PriorityLevel priority,
            FeatureUniformResourceName urn) {
        FeatureDeletionRequest request = new FeatureDeletionRequest();
        request.with(requestId, requestOwner, requestDate, state, step, priority, errors);
        request.setUrn(urn);
        request.setPriority(priority);

        return request;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public <U> U accept(IAbstractFeatureRequestVisitor<U> visitor) {
        return visitor.visitDeletionRequest(this);
    }

    public Feature getToNotify() {
        return toNotify;
    }

    public void setToNotify(Feature toNotify) {
        this.toNotify = toNotify;
    }

    public boolean isAlreadyDeleted() {
        return alreadyDeleted;
    }

    public void setAlreadyDeleted(boolean alreadyDeleted) {
        this.alreadyDeleted = alreadyDeleted;
    }
}
