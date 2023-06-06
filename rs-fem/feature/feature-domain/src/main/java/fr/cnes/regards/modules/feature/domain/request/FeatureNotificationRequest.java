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

import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * @author Kevin Marchois
 */
@Entity
@DiscriminatorValue(FeatureRequestTypeEnum.NOTIFICATION_DISCRIMINENT)
public class FeatureNotificationRequest extends AbstractFeatureRequest {

    /**
     * Should be null until it reaches {@link FeatureRequestStep#LOCAL_TO_BE_NOTIFIED}
     */
    @Column(columnDefinition = "jsonb", name = "to_notify", nullable = true)
    @Type(type = "jsonb")
    private Feature toNotify;

    @Column(name = "sessionToNotify", length = 255)
    private String sessionToNotify;

    @Column(name = "sourceToNotify", length = 255)
    private String sourceToNotify;

    @Column(name = "recipient_id", nullable = false)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ta_feature_notification_request_recipient_ids",
                     joinColumns = @JoinColumn(name = "feature_notification_request_id",
                                               foreignKey = @ForeignKey(name = "fk_ta_feature_notification_request_recipient_ids")))
    private Set<String> recipientIds;

    public static FeatureNotificationRequest build(String requestId,
                                                   String requestOwner,
                                                   OffsetDateTime requestDate,
                                                   FeatureRequestStep step,
                                                   PriorityLevel priority,
                                                   FeatureUniformResourceName urn,
                                                   RequestState state) {
        FeatureNotificationRequest request = new FeatureNotificationRequest();
        request.with(requestId, requestOwner, requestDate, priority, state, step);
        request.setStep(step);
        request.setUrn(urn);
        request.setPriority(priority);
        return request;
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

    public Set<String> getRecipientIds() {
        return recipientIds;
    }

    public void setRecipientIds(Set<String> recipientIds) {
        this.recipientIds = recipientIds;
    }

    @Override
    public <U> U accept(IAbstractFeatureRequestVisitor<U> visitor) {
        return visitor.visitNotificationRequest(this);
    }
}
