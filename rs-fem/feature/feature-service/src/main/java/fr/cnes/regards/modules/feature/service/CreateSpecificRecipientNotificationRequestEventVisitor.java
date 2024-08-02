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
package fr.cnes.regards.modules.feature.service;

import com.google.gson.Gson;
import fr.cnes.regards.modules.feature.domain.request.*;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureManagementAction;
import fr.cnes.regards.modules.notifier.dto.in.SpecificRecipientNotificationRequestEvent;

import java.util.Optional;
import java.util.Set;

/**
 * @author Stephane Cortine
 */
public class CreateSpecificRecipientNotificationRequestEventVisitor
    implements IAbstractFeatureRequestVisitor<Optional<SpecificRecipientNotificationRequestEvent>> {

    private final Gson gson;

    private final Set<AbstractFeatureRequest> visitorErrorRequests;

    public CreateSpecificRecipientNotificationRequestEventVisitor(Gson gson,

                                                                  Set<AbstractFeatureRequest> visitorErrorRequests) {
        this.gson = gson;
        this.visitorErrorRequests = visitorErrorRequests;
    }

    @Override
    public Optional<SpecificRecipientNotificationRequestEvent> visitCreationRequest(FeatureCreationRequest creationRequest) {
        return Optional.empty();
    }

    @Override
    public Optional<SpecificRecipientNotificationRequestEvent> visitDeletionRequest(FeatureDeletionRequest deletionRequest) {
        return Optional.empty();
    }

    @Override
    public Optional<SpecificRecipientNotificationRequestEvent> visitCopyRequest(FeatureCopyRequest copyRequest) {
        return Optional.empty();
    }

    @Override
    public Optional<SpecificRecipientNotificationRequestEvent> visitUpdateRequest(FeatureUpdateRequest updateRequest) {
        return Optional.empty();
    }

    @Override
    public Optional<SpecificRecipientNotificationRequestEvent> visitNotificationRequest(FeatureNotificationRequest featureNotificationRequest) {
        Feature feature = featureNotificationRequest.getToNotify();
        if (feature != null) {
            if (featureNotificationRequest.getRecipientIds().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new SpecificRecipientNotificationRequestEvent(gson.toJsonTree(feature).getAsJsonObject(),
                                                                             gson.toJsonTree(new CreateNotificationRequestEventVisitor.NotificationActionEventMetadata(
                                                                                     FeatureManagementAction.NOTIFIED,
                                                                                     featureNotificationRequest.getSourceToNotify(),
                                                                                     featureNotificationRequest.getSessionToNotify()))
                                                                                 .getAsJsonObject(),
                                                                             featureNotificationRequest.getRequestId(),
                                                                             featureNotificationRequest.getRequestOwner(),
                                                                             featureNotificationRequest.getRecipientIds()));

        } else {
            visitorErrorRequests.add(featureNotificationRequest);
            return Optional.empty();
        }
    }
}
