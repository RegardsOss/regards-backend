/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.accept;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.accept.OAISRequestType;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;

/**
 * Accept or deny request if there is another type of request currently running
 * @author Léo Mieulet
 */
@Service
public class OAISAcceptRequestService implements IOAISAcceptRequestService {

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IOAISDeletionRequestRepository oaisDeletionRequestRepository;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Override
    public boolean acceptRequest(String sessionOwner, String session, OAISRequestType requestType) {
        switch (requestType) {
            case INGEST:
                return !isDeleting(sessionOwner, session) && !isUpdating(sessionOwner, session);
            case UPDATE:
                return !isIngesting(sessionOwner, session) && !isDeleting(sessionOwner, session);
            case DELETE:
                return !isIngesting(sessionOwner, session) && !isUpdating(sessionOwner, session);
            case INDEX:
            default:
                //TODO
                return false;
        }
    }

    /**
     * @param sessionOwner
     * @param session
     * @return true when there is currently an ingestion
     */
    private boolean isIngesting(String sessionOwner, String session) {
        ArrayList<IngestRequestStep> runningStep = Lists
                .newArrayList(IngestRequestStep.LOCAL_SCHEDULED, IngestRequestStep.LOCAL_INIT,
                              IngestRequestStep.LOCAL_PRE_PROCESSING, IngestRequestStep.LOCAL_VALIDATION,
                              IngestRequestStep.LOCAL_GENERATION, IngestRequestStep.LOCAL_TAGGING,
                              IngestRequestStep.LOCAL_POST_PROCESSING, IngestRequestStep.LOCAL_FINAL,
                              IngestRequestStep.REMOTE_STORAGE_REQUESTED);
        return false;
        //        TODO
        //        return ingestRequestRepository.
        //                existsByMetadataSessionOwnerAndMetadataSessionAndStepIn(sessionOwner, session, runningStep);
    }

    /**
     * @param sessionOwner
     * @param session
     * @return true when there is currently an aip update request
     */
    private boolean isUpdating(String sessionOwner, String session) {
        return aipUpdateRequestRepository.existsBySessionOwnerAndSession(sessionOwner, session);
    }

    /**
     * @param sessionOwner
     * @param session
     * @return true when there is currently a job working to delete OAIS entites
     */
    private boolean isDeleting(String sessionOwner, String session) {
        //        return oaisDeletionRequestRepository.existsBySessionOwnerAndSession(sessionOwner, session);
        return false;
    }
}
