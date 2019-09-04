/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISessionDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.dao.SIPEntitySpecifications;
import fr.cnes.regards.modules.ingest.domain.dto.RejectedSipDto;
import fr.cnes.regards.modules.ingest.domain.request.SessionDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionSelectionMode;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * This job handles session deletion requests
 *
 * @author Marc SORDI
 */
public class SessionDeletionJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionDeletionJob.class);

    public static final String ID = "ID";

    @Autowired
    private ISessionDeletionRequestRepository deletionRequestRepository;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private INotificationClient notificationClient;

    private SessionDeletionRequest deletionRequest;

    /**
     * Limit number of SIPs to retrieve in one page.
     */
    @Value("${regards.ingest.sips.deletion.iteration-limit:100}")
    private Integer sipIterationLimit;

    private int totalPages = 0;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {

        // Retrieve deletion request
        Long database_id = getValue(parameters, ID);
        Optional<SessionDeletionRequest> oDeletionRequest = deletionRequestRepository.findById(database_id);

        if (!oDeletionRequest.isPresent()) {
            throw new JobRuntimeException(String.format("Unknown deletion request with id %d", database_id));
        }

        deletionRequest = oDeletionRequest.get();
    }

    @Override
    public void run() {
        Pageable pageRequest = PageRequest.of(0, sipIterationLimit, Sort.Direction.ASC, "id");
        boolean removeIrrevocably = deletionRequest.getDeletionMode() == SessionDeletionMode.IRREVOCABLY;
        List<SIPState> states = null;
        if (!removeIrrevocably) {
            // If the job is doing a soft removal, we can add this filter to the query
            states = new ArrayList<>(Arrays.asList(SIPState.INGESTED));
        }

        Page<SIPEntity> sipsPage;

        do {
            sipsPage = sipRepository
                    .loadAll(
                            SIPEntitySpecifications.search(deletionRequest.getProviderIds(),
                                    deletionRequest.getSipIds(), deletionRequest.getSessionOwner(),
                                    deletionRequest.getSession(), null, states, null,
                                    deletionRequest.getSelectionMode() == SessionDeletionSelectionMode.INCLUDE,
                                    null, null, null),
                            pageRequest);
            // Save number of pages to publish job advancement
            if (totalPages < sipsPage.getTotalPages()) {
                totalPages = sipsPage.getTotalPages();
            }
            sipsPage.forEach(sip -> {
                // Ask for SIP, AIP and files deletion
                RejectedSipDto rejectedSip = sipService.deleteSIPEntity(sip, removeIrrevocably);
                if (rejectedSip == null) {
                    notifySipProperlyDeleted(sip);
                } else {
                    notifySipError(sip, rejectedSip);
                }
            });
            advanceCompletion();
            pageRequest = sipsPage.nextPageable();
        } while (sipsPage.hasNext());
    }

    private void notifySipProperlyDeleted(SIPEntity sip) {
        // Publish SUCCESSFUL notif
        publisher.publish(IngestRequestEvent
                .build(deletionRequest.getRequestId(), sip.getProviderId(),
                        sip.getSipId(), RequestState.GRANTED));
    }

    private void notifySipError(SIPEntity sip, RejectedSipDto rejectedSip) {
        // Publish ERROR notif
        publisher.publish(IngestRequestEvent
                .build(deletionRequest.getRequestId(), sip.getProviderId(),
                        sip.getSipId(), RequestState.ERROR, new HashSet<>(rejectedSip.getRejectionCauses())));
    }

    @Override
    public int getCompletionCount() {
        return totalPages;
    }

}
