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
package fr.cnes.regards.modules.ingest.service.aip.scheduler;

import fr.cnes.regards.modules.ingest.service.request.IngestRequestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to schedule IngestRequest
 *
 * @author Thibaud Michaudel
 */
// Service not transactional. Transactions are created for each schedule action into IngestRequestService to avoid
// too long transactions.
@Service
public class IngestRequestSchedulerService {

    private final IngestRequestService ingestRequestService;

    @Value("${regards.ingest.request.scheduler.page.size:500}")
    private int pageSize;

    public IngestRequestSchedulerService(IngestRequestService ingestRequestService) {
        this.ingestRequestService = ingestRequestService;
    }

    /**
     * Schedule All the ingest requests in db
     */
    //This method should not be called outside the scheduler
    public void scheduleRequests() {
        boolean hasNext;
        do {
            hasNext = ingestRequestService.scheduleRequestsFirstPage(pageSize);
        } while (hasNext);
    }

    /**
     * Schedule first page of ingest requests
     */
    public void scheduleFirstPageRequests() {
        ingestRequestService.scheduleRequestsFirstPage(pageSize);
    }
}
