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

import java.time.OffsetDateTime;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;

/**
 * @author Kevin Marchois
 *
 */
@Entity
@DiscriminatorValue(AbstractFeatureRequest.NOTIFICATION)
public class FeatureNotificationRequest extends AbstractFeatureRequest {

    public static FeatureNotificationRequest build(String requestId, String requestOwner, OffsetDateTime requestDate,
            FeatureRequestStep step, PriorityLevel priority, FeatureUniformResourceName urn, RequestState state) {
        FeatureNotificationRequest request = new FeatureNotificationRequest();
        request.with(requestId, requestOwner, requestDate, priority, state, step);
        request.setStep(step);
        request.setUrn(urn);
        request.setPriority(priority);
        return request;
    }

    @Override
    public <U> U accept(IAbstractFeatureRequestVisitor<U> visitor) {
        return visitor.visitNotificationRequest(this);
    }
}
