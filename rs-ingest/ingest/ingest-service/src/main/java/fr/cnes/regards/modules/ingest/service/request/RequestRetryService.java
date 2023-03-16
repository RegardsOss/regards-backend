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
package fr.cnes.regards.modules.ingest.service.request;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@MultitenantTransactional
public class RequestRetryService implements IRequestRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestRetryService.class);

    @Autowired
    private IRequestService requestService;

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Override
    public void relaunchRequests(List<AbstractRequest> requests) {
        // Change requests states
        for (AbstractRequest request : requests) {
            // Requests must be in ERROR state
            if (request.getState() == InternalRequestState.ERROR
                || request.getState() == InternalRequestState.ABORTED) {
                // Rollback the state to TO_SCHEDULE
                requestService.switchRequestState(request);
            } else {
                LOGGER.error(
                    "Cannot relaunch the request {} because this request is neither in ERROR or ABORTED state. It was in {} state",
                    request.getId(),
                    request.getState());
            }
        }
        requestService.scheduleRequests(requests);
    }

}
