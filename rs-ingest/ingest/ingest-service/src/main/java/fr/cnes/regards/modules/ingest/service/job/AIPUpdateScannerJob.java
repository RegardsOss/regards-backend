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
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
public class AIPUpdateScannerJob extends AbstractJob<Void> {

    public static final String CRITERIA = "CRITERIA";

    private AIPUpdateParametersDto updateTaskDto;

    private int totalPages = 0;

    @Autowired
    private IAIPService aipRepository;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    /**
     * Limit number of AIPs to retrieve in one page.
     */
    @Value("${regards.ingest.aips.scan.iteration-limit:100}")
    private Integer aipIterationLimit;
    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve update task
        updateTaskDto = getValue(parameters, CRITERIA);
    }

    @Override
    public void run() {
        Pageable pageRequest = PageRequest.of(0, aipIterationLimit, Sort.Direction.ASC, "id");
        Page<AIPEntity> aipsPage;
        boolean isFirstPage = true;
        do {
            if (!isFirstPage) {
                pageRequest.next();
            }
            // Page request isn't modified as the state of entities are modified
            aipsPage = aipRepository.search(updateTaskDto.getCriteria(), pageRequest);
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
                List<AIPUpdateRequest> requests = AIPUpdateRequest.build(aip, updateTaskDto, isPending);
                aipUpdateRequestRepository.saveAll(requests);
            };
            isFirstPage = false;
            if (totalPages > 0) {
                advanceCompletion();
            }
            pageRequest.next();
        } while (aipsPage.hasNext());
    }

    @Override
    public int getCompletionCount() {
        return totalPages;
    }

}