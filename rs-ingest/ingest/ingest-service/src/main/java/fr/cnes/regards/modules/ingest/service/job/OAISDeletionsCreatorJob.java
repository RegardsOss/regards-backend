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

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionCreatorRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorPayload;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.request.OAISDeletionService;
import fr.cnes.regards.modules.ingest.service.request.RequestService;

/**
 * This job creates {@link AbstractAIPUpdateTask} task to update. It scans AIP and create for each modification a task
 *
 * @author Léo Mieulet
 */
public class OAISDeletionsCreatorJob extends AbstractJob<Void> {

    public static final String REQUEST_ID = "REQUEST_ID";

    private int totalPages = 0;

    @Autowired
    private IAIPService aipRepository;

    @Autowired
    private RequestService requestService;

    @Autowired
    private OAISDeletionService oaisDeletionRequestService;

    @Autowired
    private IOAISDeletionCreatorRepository oaisDeletionCreatorRepo;

    /**
     * Limit number of AIPs to retrieve in one page.
     */
    @Value("${regards.ingest.aips.scan.iteration-limit:100}")
    private Integer aipIterationLimit;

    private OAISDeletionCreatorRequest deletionCreator;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve deletion request
        Long databaseId = getValue(parameters, REQUEST_ID);
        Optional<OAISDeletionCreatorRequest> oDeletionRequest = oaisDeletionRequestService.searchCreator(databaseId);

        if (!oDeletionRequest.isPresent()) {
            throw new JobRuntimeException(String.format("Unknown deletion request with id %d", databaseId));
        }

        deletionCreator = oDeletionRequest.get();
    }

    @Override
    public void run() {
        Pageable pageRequest = PageRequest.of(0, aipIterationLimit, Sort.Direction.ASC, "id");
        Page<AIPEntity> aipsPage;
        boolean isFirstPage = true;
        // Set the request as running
        deletionCreator.setState(InternalRequestState.RUNNING);
        oaisDeletionCreatorRepo.save(deletionCreator);
        do {
            if (!isFirstPage) {
                pageRequest.next();
            }
            OAISDeletionCreatorPayload deletionPayload = deletionCreator.getConfig();
            aipsPage = aipRepository.findByFilters(deletionPayload, pageRequest);
            LOGGER.info("-------------------------- Nb aips to delete = {}", aipsPage.getTotalElements());
            // Save number of pages to publish job advancement
            if (totalPages < aipsPage.getTotalPages()) {
                totalPages = aipsPage.getTotalPages();
            }
            for (AIPEntity aip : aipsPage) {
                OAISDeletionRequest odr = OAISDeletionRequest.build(aip, deletionPayload.getDeletionMode(),
                                                                    deletionPayload.getDeletePhysicalFiles());
                requestService.scheduleRequest(odr);
            }
            isFirstPage = false;
            if (totalPages > 0) {
                advanceCompletion();
            }
            pageRequest.next();
        } while (aipsPage.hasNext());
        // Delete the request
        requestService.deleteRequest(deletionCreator);
    }

    @Override
    public int getCompletionCount() {
        return totalPages;
    }

}