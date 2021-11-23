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
package fr.cnes.regards.modules.workermanager.service.requests.job;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.dto.requests.SessionsRequestsInfo;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import fr.cnes.regards.modules.workermanager.service.sessions.SessionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Job to re-dispatch {@link Request}s
 *
 * @author Léo Mieulet
 */
public class DispatchRequestJob extends AbstractJob<Void> {

    /**
     * JOB Parameter key for the subset of requests to handle. We use {@link Request#getId()} here
     */
    public static final String REQUEST_DB_IDS = "request_db_ids";

    @Autowired
    private RequestService requestService;

    @Autowired
    private SessionService sessionService;

    private Set<Long> ids;

    private int nbRequestToHandle;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        // lets instantiate the plugin to use
        Type type = new TypeToken<Set<Long>>() {

        }.getType();
        ids = getValue(parameters, REQUEST_DB_IDS, type);
        nbRequestToHandle = ids.size();
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        logger.debug("[DISPATCH REQUEST JOB] Handling {} requests", nbRequestToHandle);
        List<Request> requests = requestService.searchRequests(ids);
        SessionsRequestsInfo requestsInfo = new SessionsRequestsInfo(
                requests.stream().map(Request::toDTO).collect(Collectors.toList()));
        SessionsRequestsInfo newRequestsInfo = requestService.handleRequests(requests, requestsInfo, true);

        logger.info("{} re-dispatched request(s), {} re-delayed request(s) handled in {} ms",
                    newRequestsInfo.getRequests(RequestStatus.DISPATCHED).size(), newRequestsInfo.getRequests(RequestStatus.NO_WORKER_AVAILABLE).size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getCompletionCount() {
        return nbRequestToHandle > 0 ? nbRequestToHandle : super.getCompletionCount();
    }
}