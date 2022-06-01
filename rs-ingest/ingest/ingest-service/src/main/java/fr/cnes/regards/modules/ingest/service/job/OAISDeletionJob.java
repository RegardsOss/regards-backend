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
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.DeletionRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.service.notification.AIPNotificationService;
import fr.cnes.regards.modules.ingest.service.request.OAISDeletionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Job to run deletion of a given {@link AIPEntity}.<br/>
 * <ul>
 * <li> Ask for remote file storage deletion if asked for</li>
 * <li> Delete AIP or mark it as DELETED </li>
 * <li> Delete SIP or mark it as DELETED </li>
 * </ul>
 *
 * @author Sébastien Binda
 */
public class OAISDeletionJob extends AbstractJob<Void> {

    public static final String OAIS_DELETION_REQUEST_IDS = "OAIS_DELETION_REQUEST_IDS";

    private List<OAISDeletionRequest> requests = Lists.newArrayList();

    @Autowired
    private OAISDeletionService oaisDeletionRequestService;

    @Autowired
    private AIPNotificationService aipNotificationService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve param
        Type type = new TypeToken<List<Long>>() {

        }.getType();
        List<Long> deleteRequestIds = getValue(parameters, OAIS_DELETION_REQUEST_IDS, type);
        // Retrieve list of AIP save metadata requests to handle
        requests = oaisDeletionRequestService.searchRequests(deleteRequestIds);
    }

    @Override
    public void run() {
        // INIT
        int nbRequestsToHandle = this.requests.size(); // nb of requests to handle (retry requests + deletions)
        logger.debug("Running job for {} OAISDeletionRequest(s) requests", nbRequestsToHandle);
        long start = System.currentTimeMillis();

        // NOTIFICATION RETRY
        // filter out requests with notification step (in case of retry)
        Set<AbstractRequest> notificationRetryRequests = requests.stream()
                                                                 .filter(req -> req.getStep()
                                                                                == DeletionRequestStep.REMOTE_NOTIFICATION_ERROR)
                                                                 .collect(Collectors.toSet());
        if (!notificationRetryRequests.isEmpty()) {
            // remove notifications from requests to process and send them again
            this.requests.removeAll(notificationRetryRequests);
            aipNotificationService.sendRequestsToNotifier(notificationRetryRequests);
        }

        // DELETION
        if (!this.requests.isEmpty()) {
            // run process of deletion
            oaisDeletionRequestService.runDeletion(requests, this);
        }

        logger.debug("Job handled for {} OAISDeletionRequest(s) requests in {}ms",
                     nbRequestsToHandle,
                     System.currentTimeMillis() - start);
    }

    @Override
    public int getCompletionCount() {
        return requests.size();
    }

}