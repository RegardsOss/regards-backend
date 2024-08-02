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
package fr.cnes.regards.modules.workermanager.service.requests.job;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.domain.request.SearchRequestParameters;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.requests.scan.RequestScanService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Job to scan {@link Request}s before re-dispatching or deleting requests
 *
 * @author Léo Mieulet
 */
public class ScanRequestJob extends AbstractJob<Void> {

    /**
     * JOB Parameter key for the requests new status
     */
    public static final String REQUEST_NEW_STATUS = "request_status";

    /**
     * JOB Parameter key for filters to use while searching {@link Request}s
     */
    public static final String FILTERS = "filters";

    public static final Long MAX_TASK_WAIT_DURING_JOB = 1200L; // In second

    @Autowired
    private RequestScanService requestScanService;

    private RequestStatus newStatus;

    private SearchRequestParameters searchRequestFilters;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        // lets instantiate plugin parameters
        searchRequestFilters = parameters.get(FILTERS).getValue();
        newStatus = parameters.get(REQUEST_NEW_STATUS).getValue();
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        logger.debug("[SCAN REQUEST JOB] Starting");

        // Run scan
        try {
            requestScanService.scanUsingFilters(searchRequestFilters, newStatus, MAX_TASK_WAIT_DURING_JOB);
        } catch (Throwable e) {
            logger.error("Business error", e);
            throw new JobRuntimeException(e);
        }

        logger.debug("[SCAN REQUEST JOB] Handled in {} ms", System.currentTimeMillis() - start);
    }

}