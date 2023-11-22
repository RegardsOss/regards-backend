/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ingest.domain.request.dissemination.AipDisseminationRequest;
import fr.cnes.regards.modules.ingest.service.AipDisseminationService;
import fr.cnes.regards.modules.ingest.service.notification.AIPNotificationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * This job is used to send an amqp message to notify by {@link AipDisseminationRequest} in parameter.
 * Every dissemination request state are updated to a special state. This state means that ingest has just send a
 * request to notifier, and is now waiting for notifier response.
 *
 * @author Thomas GUILLOU
 **/
public class AipDisseminationJob extends AbstractJob<Void> {

    public static final String AIP_DISSEMINATION_REQUEST_IDS = "AIP_DISSEMINATION_REQUEST_IDS";

    private List<AipDisseminationRequest> aipDisseminationRequests;

    @Autowired
    private AipDisseminationService aipDisseminationService;

    @Autowired
    private AIPNotificationService aipNotificationService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve param
        List<Long> aipRequestIds = getValue(parameters, AIP_DISSEMINATION_REQUEST_IDS, new TypeToken<List<Long>>() {

        }.getType());
        aipDisseminationRequests = aipDisseminationService.findAllById(aipRequestIds);
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        logger.debug("[AIP DISSEMINATION JOB] Start dissemination notification for {} aips",
                     aipDisseminationRequests.size());
        aipNotificationService.sendRequestsToNotifier(Sets.newHashSet(aipDisseminationRequests));
        logger.debug("[AIP DISSEMINATION JOB] End, {} dissemination notification send in {} ms",
                     aipDisseminationRequests.size(),
                     System.currentTimeMillis() - start);
    }

}
