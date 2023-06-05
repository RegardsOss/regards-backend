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

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMode;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestParameters;
import fr.cnes.regards.modules.ingest.service.request.RequestDeletionService;
import fr.cnes.regards.modules.ingest.service.request.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This job handles request deletion
 *
 * <br>This job cannot be interrupted as it is simply handling operation on requests. It basically does nothing.
 *
 * @author Léo Mieulet
 */
public class RequestDeletionJob extends AbstractJob<Void> {

    public static final String CRITERIA_JOB_PARAM_NAME = "CRITERIA";

    @Autowired
    private RequestService requestService;

    @Autowired
    private RequestDeletionService requestDeletionService;

    /**
     * Limit number of requests to retrieve in one page.
     */
    @Value("${regards.request.deletion.iteration-limit:1000}")
    private Integer requestIterationLimit;

    private int totalPages = 0;

    private SearchRequestParameters criteria;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {

        // Retrieve deletion payload
        criteria = getValue(parameters, CRITERIA_JOB_PARAM_NAME);
    }

    @Override
    public void run() {
        logger.debug("[REQUEST DELETION JOB] Running job ...");
        long start = System.currentTimeMillis();
        int nbRequestsDeleted = 0;
        Pageable pageRequest = PageRequest.of(0, requestIterationLimit, Sort.Direction.ASC, "id");
        if (criteria.getRequestStates() == null) {
            criteria.withRequestStatesExcluded(Set.of(InternalRequestState.RUNNING));
        } else {
            criteria.getRequestStates().setValues(getValidRequestStates(criteria.getRequestStates()));
        }
        Page<AbstractRequest> requestsPage;
        do {
            // Page request isn't modified as entities are removed on every page fetched
            requestsPage = requestService.findRequests(criteria, pageRequest);
            // Save number of pages to publish job advancement
            if (totalPages < requestsPage.getTotalPages()) {
                totalPages = requestsPage.getTotalPages();
            }
            requestDeletionService.deleteRequests(requestsPage.getContent());
            advanceCompletion();
            nbRequestsDeleted += requestsPage.getNumberOfElements();
        } while (requestsPage.hasNext());
        logger.debug("[REQUEST DELETION JOB] Job handled for {} AbstractRequest(s) in {}ms",
                     nbRequestsDeleted,
                     System.currentTimeMillis() - start);
    }

    /**
     * Get a safe list of RequestType criteria that can be used to make the research on Request.
     * As it is not safe to delete a ${@link InternalRequestState#RUNNING}
     */
    public static Collection<InternalRequestState> getValidRequestStates(ValuesRestriction<InternalRequestState> requestStatesValuesRestriction) {
        Collection<InternalRequestState> requestStates = requestStatesValuesRestriction.getValues();
        if (requestStatesValuesRestriction.getMode().equals(ValuesRestrictionMode.INCLUDE)) {
            Collection<InternalRequestState> requestStatesResult = requestStates;
            if (requestStates.isEmpty()) {
                requestStatesResult = List.of(InternalRequestState.values());
            }
            // RUNNING should not be present on INCLUDE
            return requestStatesResult.stream()
                                      .filter(requestType -> !requestType.equals(InternalRequestState.RUNNING))
                                      .toList();
        }
        // RUNNING should be present on EXCLUDE
        if (!requestStates.contains(InternalRequestState.RUNNING)) {
            List<InternalRequestState> internalRequestStates = Lists.newArrayList(requestStates);
            internalRequestStates.add(InternalRequestState.RUNNING);
            return internalRequestStates;
        }
        return requestStates;
    }

    @Override
    public int getCompletionCount() {
        // Do not return 0 value. Completion must be a positive integer.
        return totalPages > 0 ? totalPages : 1;
    }

}
