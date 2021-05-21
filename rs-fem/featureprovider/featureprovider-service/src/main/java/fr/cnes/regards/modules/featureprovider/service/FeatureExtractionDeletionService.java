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
package fr.cnes.regards.modules.featureprovider.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.featureprovider.dao.IFeatureExtractionRequestRepository;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequest;
import fr.cnes.regards.modules.featureprovider.service.conf.FeatureProviderConfigurationProperties;
import fr.cnes.regards.modules.featureprovider.service.session.SessionNotifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Service to schedule a {@link FeatureExtractionDeletionJob} and delete {@link FeatureExtractionRequest}
 * with state DENIED or ERROR
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class FeatureExtractionDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureExtractionDeletionService.class);

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IFeatureExtractionRequestRepository requestRepo;

    @Autowired
    private FeatureProviderConfigurationProperties confProperties;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private FeatureExtractionDeletionService self;

    /**
     * Schedule a {@link FeatureExtractionDeletionJob} to delete {@link FeatureExtractionRequest}
     *
     * @param source  name of the source requested for deletion
     * @param session optional name of the session requested for deletion
     */
    public void scheduleDeletion(String source, Optional<String> session) {
        JobInfo jobInfo = new JobInfo(true);
        jobInfo.setPriority(PriorityLevel.NORMAL.getPriorityLevel());
        jobInfo.setParameters(FeatureExtractionDeletionJob.getParameters(source, session));
        jobInfo.setClassName(FeatureExtractionDeletionJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
    }

    /**
     * Handle the deletion of requests by source and eventually session
     *
     * @param sourceName  name of source
     * @param sessionName name of session, could be null
     * @return number of requests deleted
     */
    public long deleteFeatureExtractionRequest(String sourceName, String sessionName) {
        // init parameters
        long nbDeletedReq = 0;
        Page<FeatureExtractionRequest> pageExtractionReq;
        Pageable pageToRequest = PageRequest.of(0, confProperties.getMaxBulkSize());
        // define requests states to delete
        Set<RequestState> states = Sets.newHashSet(RequestState.DENIED, RequestState.ERROR);

        // DELETE AND NOTIFY FEATURE EXTRACTION REQUESTS
        do {
            // find all requests by source or by source and session
            if (sessionName == null) {
                pageExtractionReq = requestRepo.findByMetadataSessionOwnerAndStateIn(sourceName, states, pageToRequest);
            } else {
                pageExtractionReq = requestRepo
                        .findByMetadataSessionOwnerAndMetadataSessionAndStateIn(sourceName, sessionName, states,
                                                                                pageToRequest);
            }
            // delete and notify
            List<FeatureExtractionRequest> requests = pageExtractionReq.getContent();
            if (!requests.isEmpty()) {
                self.deleteAndNotify(sourceName, requests);
                // count number of requests deleted
                nbDeletedReq += pageExtractionReq.getNumberOfElements();
            }
        } while (!Thread.currentThread().isInterrupted() && pageExtractionReq.hasNext());

        // log if thread was interrupted
        if (Thread.currentThread().isInterrupted()) {
            LOGGER.debug("{} thread has been interrupted", this.getClass().getName());
            // interrupt thread
            Thread.currentThread().interrupt();
        }
        return nbDeletedReq;
    }

    /**
     * Delete {@link FeatureExtractionRequest} and notify their deletion
     *
     * @param sourceName name of the source
     * @param requests   list of requests to delete
     */
    public void deleteAndNotify(String sourceName, List<FeatureExtractionRequest> requests) {
        // delete extraction requests
        requestRepo.deleteAll(requests);
        // notify deletion - regroup requests by session
        Map<String, List<FeatureExtractionRequest>> reqBySession = requests.stream()
                .collect(Collectors.groupingBy(req -> req.getMetadata().getSession()));

        for (Map.Entry<String, List<FeatureExtractionRequest>> requestList : reqBySession.entrySet()) {
            // count the number of requests by state
            long nbRefusedReq = requestList.getValue().stream()
                    .filter(req -> req.getState().equals(RequestState.DENIED)).count();
            long nbErrorReq = requestList.getValue().stream().filter(req -> req.getState().equals(RequestState.ERROR))
                    .count();

            // notify if requests were deleted
            if (nbRefusedReq != 0) {
                this.sessionNotifier.decrementRequestRefused(sourceName, requestList.getKey(), nbRefusedReq);
            }
            if (nbErrorReq != 0) {
                this.sessionNotifier.decrementRequestErrors(sourceName, requestList.getKey(), nbErrorReq);
            }
        }
    }
}