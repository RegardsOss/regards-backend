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

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * @author Kevin Marchois
 */
@Entity
@DiscriminatorValue(FeatureRequestTypeEnum.DELETION_DISCRIMINENT)
public class FeatureDeletionRequest extends AbstractFeatureRequest {

    /**
     * Should be null until it reaches {@link FeatureRequestStep#LOCAL_TO_BE_NOTIFIED}
     */
    @Column(columnDefinition = "jsonb", name = "to_notify", nullable = true)
    @Type(JsonBinaryType.class)
    private Feature toNotify;

    @Column(name = "sessionToNotify", length = 255)
    private String sessionToNotify;

    @Column(name = "sourceToNotify", length = 255)
    private String sourceToNotify;

    /**
     * This is used to notify user. Should only be setted by deletion process
     */
    @Column(name = "already_deleted")
    private boolean alreadyDeleted;

    /**
     * This parameter can be used to force deletion of feature waiting for a blocking dissemination ack.
     */
    @Column(name = "force_deletion")
    private boolean forceDeletion;

    public static FeatureDeletionRequest build(String requestId,
                                               String requestOwner,
                                               OffsetDateTime requestDate,
                                               RequestState state,
                                               Set<String> errors,
                                               FeatureRequestStep step,
                                               PriorityLevel priority,
                                               FeatureUniformResourceName urn) {
        FeatureDeletionRequest request = new FeatureDeletionRequest();
        request.with(requestId, requestOwner, requestDate, state, step, priority, errors);
        request.setUrn(urn);
        request.setPriority(priority);

        return request;
    }

    @Override
    public <U> U accept(IAbstractFeatureRequestVisitor<U> visitor) {
        return visitor.visitDeletionRequest(this);
    }

    public Feature getToNotify() {
        return toNotify;
    }

    public void setToNotify(Feature toNotify, String sourceToNotify, String sessionToNotify) {
        this.toNotify = toNotify;
        this.sessionToNotify = sessionToNotify;
        this.sourceToNotify = sourceToNotify;
    }

    public boolean isAlreadyDeleted() {
        return alreadyDeleted;
    }

    public void setAlreadyDeleted(boolean alreadyDeleted) {
        this.alreadyDeleted = alreadyDeleted;
    }

    public String getSessionToNotify() {
        return sessionToNotify;
    }

    public String getSourceToNotify() {
        return sourceToNotify;
    }

    public boolean isForceDeletion() {
        return forceDeletion;
    }
}
