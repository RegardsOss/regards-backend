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
package fr.cnes.regards.modules.ingest.service.job;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.request.OAISDeletionService;
import fr.cnes.regards.modules.ingest.service.request.RequestService;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;

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
    private RequestService requestService;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private IOAISDeletionRequestRepository deletionRequestRepository;

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
        Iterator<OAISDeletionRequest> requestIter = requests.iterator();
        boolean interrupted = Thread.currentThread().isInterrupted();
        Set<OAISDeletionRequest> errors = new HashSet<>();
        while (requestIter.hasNext() && !interrupted) {
            OAISDeletionRequest request = requestIter.next();
            AIPEntity aipToDelete = request.getAip();
            SIPEntity sipToDelete = aipToDelete.getSip();
            try {
                if (request.isDeleteFiles() && !request.isRequestFilesDeleted()) {
                    aipService.scheduleLinkedFilesDeletion(request);
                } else {
                    // Start by deleting the request itself
                    requestService.deleteRequest(request);
                    aipService.processDeletion(sipToDelete.getSipId(),
                                               request.getDeletionMode() == SessionDeletionMode.IRREVOCABLY);
                    sipService.processDeletion(sipToDelete.getSipId(),
                                               request.getDeletionMode() == SessionDeletionMode.IRREVOCABLY);
                }
            } catch (Exception e) {
                String errorMsg = String.format("Deletion request %s of AIP %s could not be executed", request.getId(),
                                                request.getAip().getAipId());
                LOGGER.error(errorMsg, e);
                request.setState(InternalRequestState.ERROR);
                request.addError(errorMsg);
                errors.add(request);
            }
            advanceCompletion();
            interrupted = Thread.currentThread().isInterrupted();
        }
        // abort requests that could not be handled
        ArrayList<OAISDeletionRequest> aborted = new ArrayList<>();
        while (requestIter.hasNext()) {
            OAISDeletionRequest request = requestIter.next();
            request.setState(InternalRequestState.ABORTED);
            aborted.add(request);
        }
        interrupted = Thread.interrupted();
        deletionRequestRepository.saveAll(errors);
        deletionRequestRepository.saveAll(aborted);
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public int getCompletionCount() {
        return requests.size();
    }

}