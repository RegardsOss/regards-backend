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
package fr.cnes.regards.modules.ingest.service.job;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestParameters;
import fr.cnes.regards.modules.ingest.service.request.IRequestRetryService;
import fr.cnes.regards.modules.ingest.service.request.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.*;

/**
 * This job handles request retry
 *
 * <br>This job cannot be interrupted as it is simply handling operation on requests. It basically does nothing.
 *
 * @author Léo Mieulet
 */
public class RequestRetryJob extends AbstractJob<Void> {

    public static final String CRITERIA_JOB_PARAM_NAME = "CRITERIA";

    @Autowired
    private RequestService requestService;

    @Autowired
    private IRequestRetryService retryRequestService;

    /**
     * Limit number of requests to retrieve in one page.
     */
    @Value("${regards.request.retry.iteration-limit:1000}")
    private Integer requestIterationLimit;

    private int totalPages = 0;

    private SearchRequestParameters criteria;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {

        // Retrieve request criteria payload
        criteria = getValue(parameters, CRITERIA_JOB_PARAM_NAME);
    }

    @Override
    public void run() {
        logger.debug("Running job ...");
        long start = System.currentTimeMillis();
        Pageable pageRequest = PageRequest.of(0, requestIterationLimit, Sort.Direction.ASC, "id");
        // Override state in filter
        criteria.withRequestStatesExcluded(null)
                .withRequestStatesIncluded(Set.of(InternalRequestState.ERROR, InternalRequestState.ABORTED));
        int nbRelaunchedRequests = 0;
        Page<AbstractRequest> requestsPage;
        do {
            // Page request isn't modified, as entities doesn't keep the ERROR state, on every page fetched
            requestsPage = requestService.findRequests(criteria, pageRequest);
            // Save number of pages to publish job advancement
            if (totalPages < requestsPage.getTotalPages()) {
                totalPages = requestsPage.getTotalPages();
            }
            // Sort out requests by type
            Map<String, List<AbstractRequest>> byRequestType = new HashMap<>();
            for (AbstractRequest ar : requestsPage) {
                if (!byRequestType.containsKey(ar.getDtype())) {
                    byRequestType.put(ar.getDtype(), new ArrayList<>());
                }
                byRequestType.get(ar.getDtype()).add(ar);
            }
            // Call the service with each list of requests sorted by type
            for (List<AbstractRequest> requestsByType : byRequestType.values()) {
                retryRequestService.relaunchRequests(requestsByType);
            }
            nbRelaunchedRequests += byRequestType.size();
            advanceCompletion();
        } while (requestsPage.hasNext());
        logger.debug("Job handled for {} AbstractRequest(s) in {}ms",
                     nbRelaunchedRequests,
                     System.currentTimeMillis() - start);
    }

    @Override
    public int getCompletionCount() {
        // Do not return 0 value. Completion must be a positive integer.
        return totalPages > 0 ? totalPages : 1;
    }

}
