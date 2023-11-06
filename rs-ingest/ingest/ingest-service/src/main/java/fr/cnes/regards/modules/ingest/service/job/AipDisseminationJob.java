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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ingest.dao.IAipDisseminationRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.dissemination.AipDisseminationRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.service.AipDisseminationService;
import fr.cnes.regards.modules.ingest.service.notification.AIPNotificationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

record AipDisseminationRequestLight(AIPEntity aip,
                                    List<String> recipient) {

}

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
    private IAipDisseminationRequestRepository aipDisseminationRequestRepository;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private Gson gson;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve param
        Type type = new TypeToken<List<Long>>() {

        }.getType();
        List<Long> aipRequestIds = getValue(parameters, AIP_DISSEMINATION_REQUEST_IDS, type);
        // Retrieve list of AIP save metadata requests to handle
        aipDisseminationRequests = aipDisseminationService.searchRequests(aipRequestIds);
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        logger.debug("[AIP DISSEMINATION JOB] Start dissemination notification for {} aips",
                     aipDisseminationRequests.size());
        for (AipDisseminationRequest disseminationRequest : aipDisseminationRequests) {
            publisher.publish(new NotificationRequestEvent(gson.toJsonTree(new AipDisseminationRequestLight(
                disseminationRequest.getAip(),
                disseminationRequest.getRecipients())).getAsJsonObject(),
                                                           gson.toJsonTree(new AIPNotificationService.NotificationActionEventMetadata(
                                                                   RequestTypeConstant.AIP_DISSEMINATION_VALUE))
                                                               .getAsJsonObject(),
                                                           disseminationRequest.getCorrelationId(),
                                                           disseminationRequest.getAip().getSessionOwner()));
            disseminationRequest.setState(InternalRequestState.WAITING_NOTIFIER_DISSEMINATION_RESPONSE);
        }
        aipDisseminationRequestRepository.saveAll(aipDisseminationRequests);
        logger.debug("[AIP DISSEMINATION JOB] End, {} dissemination notification send in {} ms",
                     aipDisseminationRequests.size(),
                     System.currentTimeMillis() - start);
    }

}
