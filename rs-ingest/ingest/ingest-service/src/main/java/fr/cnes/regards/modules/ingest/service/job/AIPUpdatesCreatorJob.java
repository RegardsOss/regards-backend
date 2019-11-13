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


import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdatesCreatorRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdatesCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * This job creates {@link AbstractAIPUpdateTask} task to update. It scans AIP and create for each modification a task
 *
 * @author Léo Mieulet
 */
public class AIPUpdatesCreatorJob extends AbstractJob<Void> {

    public static final String REQUEST_ID = "REQUEST_ID";

    private AIPUpdatesCreatorRequest request;

    private int totalPages = 0;

    @Autowired
    private IAIPService aipRepository;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private IAIPUpdatesCreatorRepository aipUpdatesCreatorRepository;

    /**
     * Limit number of AIPs to retrieve in one page.
     */
    @Value("${regards.ingest.aips.scan.iteration-limit:100}")
    private Integer aipIterationLimit;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve update request id
        Long requestId = getValue(parameters, REQUEST_ID);
        // Retrieve the request
        Optional<AIPUpdatesCreatorRequest> oDeletionRequest = aipUpdatesCreatorRepository.findById(requestId);
        if (!oDeletionRequest.isPresent()) {
            throw new JobRuntimeException(String.format("Unknown deletion request with id %d", requestId));
        }
        request = oDeletionRequest.get();
    }

    @Override
    public void run() {
        Pageable pageRequest = PageRequest.of(0, aipIterationLimit, Sort.Direction.ASC, "id");
        Page<AIPEntity> aipsPage;
        boolean isFirstPage = true;
        // Set the request as running
        request.setState(InternalRequestStep.RUNNING);
        aipUpdatesCreatorRepository.save(request);
        do {
            if (!isFirstPage) {
                pageRequest.next();
            }
            AIPUpdateParametersDto updateTask = request.getConfig();
            aipsPage = aipRepository.search(updateTask.getCriteria(), pageRequest);
            // Save number of pages to publish job advancement
            if (totalPages < aipsPage.getTotalPages()) {
                totalPages = aipsPage.getTotalPages();
            }
            List<AIPEntity> aipsPageContent = aipsPage.getContent();

            // Test if there is some AIPs referenced by some running requests
            List<Long> aipIds = aipsPageContent.stream().map(wr -> wr.getId()).collect(Collectors.toList());
            List<AIPUpdateRequest> runningRequests = aipUpdateRequestRepository.findRunningRequestAndAipIdIn(aipIds);
            // Create the list of AIP id (and not aipId!)
            List<Long> runningAIPIds = runningRequests.stream().map(wr -> wr.getAip().getId()).collect(Collectors.toList());

            for (AIPEntity aip : aipsPageContent) {
                // Create the request as pending if there is already a running request
                boolean isPending = runningAIPIds.contains(aip.getId());
                List<AIPUpdateRequest> requests = AIPUpdateRequest.build(aip, updateTask, isPending);
                aipUpdateRequestRepository.saveAll(requests);
            };
            isFirstPage = false;
            if (totalPages > 0) {
                advanceCompletion();
            }
            pageRequest.next();
        } while (aipsPage.hasNext());
        // Delete the request
        aipUpdatesCreatorRepository.delete(request);
    }

    @Override
    public int getCompletionCount() {
        return totalPages;
    }

}