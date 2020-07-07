/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.feature.dto.event.in.AbstractRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.service.logger.FeatureLogger;

/**
 * @author Marc SORDI
 */
public abstract class AbstractFeatureService implements IFeatureDeniedService, IRequestValidation {

    @Autowired
    private IPublisher publisher;

    @Override
    public void validateRequest(AbstractRequestEvent event, Errors errors) {
        if (!event.hasRequestId()) {
            errors.reject("missing.request.id.header", "Missing request id header");
        }
        if (!event.hasRequestDate()) {
            errors.reject("missing.request.date.header", "Missing request date header");
        }
        if (!event.hasRequestOwner()) {
            errors.reject("missing.request.owner.header", "Missing request owner header");
        }
    }

    @Override
    public boolean denyMessage(FeatureRequestType type, Message message, String errorMessage) {

        String requestId = AbstractRequestEvent.getRequestId(message.getMessageProperties());
        if (requestId == null) {
            return false;
        }

        String requestOwner = AbstractRequestEvent.getRequestOwner(message.getMessageProperties());
        // Monitoring log
        FeatureLogger.creationDenied(requestOwner, requestId, null, Sets.newHashSet(errorMessage));
        // Publish DENIED request
        publisher.publish(FeatureRequestEvent.build(type, requestId, requestOwner, null, null, RequestState.DENIED,
                                                    Sets.newHashSet(errorMessage)));
        // FIXME is it useful, really?
        // metrics.count(null, null, FeatureCreationState.CREATION_REQUEST_DENIED);
        return true;
    }
}
