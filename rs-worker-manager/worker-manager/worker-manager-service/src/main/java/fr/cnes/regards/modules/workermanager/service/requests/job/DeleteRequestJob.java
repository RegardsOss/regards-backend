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

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import fr.cnes.regards.modules.workermanager.service.sessions.SessionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * Job to delete {@link Request}s
 *
 * @author Léo Mieulet
 */
public class DeleteRequestJob extends AbstractJob<Void> {

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
        // Initiate the job progress manager
        logger.debug("[DELETE REQUEST JOB] Delete request job for {} requests", nbRequestToHandle);

        requestService.deleteRequests(requestService.searchRequests(ids));

        logger.info("[DELETE REQUEST JOB] {} request(s) deleted in {} ms",
                    nbRequestToHandle,
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getCompletionCount() {
        return nbRequestToHandle > 0 ? nbRequestToHandle : super.getCompletionCount();
    }
}