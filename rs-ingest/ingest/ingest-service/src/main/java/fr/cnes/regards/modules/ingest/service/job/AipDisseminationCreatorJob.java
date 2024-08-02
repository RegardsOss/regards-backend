/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.ingest.dao.IAipDisseminationCreatorRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.dissemination.AipDisseminationCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.dissemination.AipDisseminationRequest;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.service.AipDisseminationService;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.request.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This job creates DisseminationRequest task for each aips matching criteria in DisseminationRequest.
 *
 * <br>This job cannot be interrupted as it is a creator for other jobs. It basically does nothing.
 *
 * @author Thomas GUILLOU
 **/
public class AipDisseminationCreatorJob extends AbstractJob<Void> {

    public static final String REQUEST_ID = "REQUEST_ID";

    @Autowired
    private IAipDisseminationCreatorRepository aipDisseminationCreatorRepository;

    @Autowired
    private RequestService requestService;

    @Autowired
    private AipDisseminationService aipDisseminationService;

    @Autowired
    private IAIPService aipService;

    /**
     * Limit number of AIPs to retrieve in one page.
     */
    @Value("${regards.ingest.aips.dissemination.bulk:1000}")
    private Integer numberOfAipMaxPerPage;

    private AipDisseminationCreatorRequest disseminationCreatorRequest;

    private int totalPages = 0;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve update request id
        Long requestId = getValue(parameters, REQUEST_ID);
        // Retrieve the request
        Optional<AipDisseminationCreatorRequest> oDisseminationRequest = aipDisseminationCreatorRepository.findById(
            requestId);
        if (oDisseminationRequest.isEmpty()) {
            throw new JobRuntimeException(String.format("Unknown dissemination request with id %d", requestId));
        }
        disseminationCreatorRequest = oDisseminationRequest.get();
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        logger.info("[AIP DISSEMINATION CREATOR JOB]: start creating requests with recipients {}",
                    disseminationCreatorRequest.getRequest().recipients());

        setAndSaveCreatorRequestToRunningState();
        createDisseminationRequestForAipsMatching(disseminationCreatorRequest.getRequest().filters(),
                                                  disseminationCreatorRequest.getRequest().recipients(),
                                                  numberOfAipMaxPerPage);
        // Delete the creator request when all dissemination requests are created
        requestService.deleteRequest(disseminationCreatorRequest);
        logger.info("[AIP DISSEMINATION CREATOR JOB]: end creating requests in {} ms",
                    System.currentTimeMillis() - start);
    }

    private void setAndSaveCreatorRequestToRunningState() {
        disseminationCreatorRequest.setState(InternalRequestState.RUNNING);
        aipDisseminationCreatorRepository.save(disseminationCreatorRequest);
    }

    private void createDisseminationRequestForAipsMatching(SearchAIPsParameters filters,
                                                           Set<String> recipients,
                                                           Integer numberOfAipMaxPerPage) {
        Pageable pageRequest = PageRequest.of(0, numberOfAipMaxPerPage, Sort.Direction.ASC, "id");
        Page<AIPEntity> aipsPage = aipService.findByFilters(filters, pageRequest);
        createAndScheduleDisseminationRequestFor(aipsPage.getContent(), recipients);
        totalPages = aipsPage.getTotalPages();
        while (aipsPage.hasNext()) {
            pageRequest = aipsPage.nextPageable();
            aipsPage = aipService.findByFilters(filters, pageRequest);
            createAndScheduleDisseminationRequestFor(aipsPage.getContent(), recipients);
            advanceCompletion();
        }
    }

    private void createAndScheduleDisseminationRequestFor(List<AIPEntity> aips, Set<String> recipients) {
        logger.debug("Scheduling dissemination of {} aips", aips.size());
        List<AbstractRequest> disseminationRequests = aips.stream()
                                                          .map(aip -> AipDisseminationRequest.build(aip, recipients))
                                                          .collect(Collectors.toList());
        requestService.scheduleRequests(disseminationRequests);
    }

    @Override
    public int getCompletionCount() {
        return totalPages;
    }
}
