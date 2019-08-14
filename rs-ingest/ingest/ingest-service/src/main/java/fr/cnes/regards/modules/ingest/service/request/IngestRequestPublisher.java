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
package fr.cnes.regards.modules.ingest.service.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.domain.dto.RequestType;
import fr.cnes.regards.modules.ingest.domain.request.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;

/**
 *
 * Publisher dedicated to request event publication strategy
 *
 * @author Marc SORDI
 *
 */
@Component
@MultitenantTransactional
public class IngestRequestPublisher {

    @Autowired
    private IPublisher publisher;

    public void publishIngestRequest(IngestRequest request) {
        publishIngestRequest(request, null);
    }

    public void publishIngestRequest(IngestRequest request, @Nullable SIPEntity entity) {
        publisher.publish(IngestRequestEvent.build(request.getRequestId(),
                                                   request.getSip() != null ? request.getSip().getId() : null,
                                                   entity != null ? entity.getSipId() : null, request.getState(),
                                                   RequestType.INGEST, request.getErrors()));
    }
}
