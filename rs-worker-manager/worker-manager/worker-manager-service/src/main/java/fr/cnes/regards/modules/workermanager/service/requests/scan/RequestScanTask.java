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
package fr.cnes.regards.modules.workermanager.service.requests.scan;

import com.google.common.collect.Lists;
import fr.cnes.regards.modules.workermanager.domain.request.SearchRequestParameters;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scans requests to update their state to an intermediate status before launching update/delete job
 * Only one scan task can be executed at the same time, but this task should be fast
 *
 * @author Léo Mieulet
 */
public class RequestScanTask implements Task {

    // Statuses of requests that can are deletable/re-dispatchable
    public static final List<RequestStatus> BLOCKED_REQUESTS_STATUSES = Lists.newArrayList(RequestStatus.ERROR, RequestStatus.NO_WORKER_AVAILABLE,
                                                                                           RequestStatus.INVALID_CONTENT);

    private final RequestScanService requestScanService;

    private final SearchRequestParameters filters;

    private final RequestStatus newStatus;

    public RequestScanTask(RequestScanService requestScanService, SearchRequestParameters filters,
            RequestStatus newStatus) {
        this.requestScanService = requestScanService;
        this.filters = filters;
        this.newStatus = newStatus;
    }

    @Override
    public void call() throws Throwable {
        LockAssert.assertLocked();
        // Ensures filter cannot scan entities created after its launch, if not already done
        if (filters.getCreationDate().getBefore() == null) {
            filters.withCreationDateBefore(OffsetDateTime.now());
        }

        // Override filter's status when:
        // - no status or empty status
        // - status invalid
        if (filters.getStatuses() == null ||
                filters.getStatuses().getValues().isEmpty() ||
                filters.getStatuses().getValues().stream().anyMatch(status -> !BLOCKED_REQUESTS_STATUSES.contains(status))) {
            filters.withStatusesIncluded(BLOCKED_REQUESTS_STATUSES);
        }

        requestScanService.updateRequestsToStatusAndScheduleJob(filters, newStatus);
    }
}
