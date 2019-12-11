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

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.ingest.dao.AIPEntitySpecification;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.service.request.OAISDeletionRequestService;
import fr.cnes.regards.modules.ingest.service.request.RequestService;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * This job handles session deletion requests
 *
 * @author Léo Mieulet
 */
public class OAISEntityDeletionJob extends AbstractJob<Void> {

    public static final String ID = "ID";

    @Autowired
    private OAISDeletionRequestService oaisDeletionRequestService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private ISIPService sipService;

    private OAISDeletionRequest deletionRequest;

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
        Long databaseId = getValue(parameters, ID);
        Optional<OAISDeletionRequest> oDeletionRequest = oaisDeletionRequestService.search(databaseId);

        if (!oDeletionRequest.isPresent()) {
            throw new JobRuntimeException(String.format("Unknown deletion request with id %d", databaseId));
        }

        deletionRequest = oDeletionRequest.get();
    }

    @Override
    public void run() {
        Pageable pageRequest = PageRequest.of(0, sipIterationLimit, Sort.Direction.ASC, "id");
        List<SIPState> states = new ArrayList<>(Arrays.asList(SIPState.INGESTED, SIPState.STORED));

        Page<AIPEntity> aipsPage;
        do {
            // Page request isn't modified as the state of entities are modified
            aipsPage = aipRepository.findAll(AIPEntitySpecification
                    .searchAll(deletionRequest.getConfig(), pageRequest), pageRequest);
            // Save number of pages to publish job advancement
            if (totalPages < aipsPage.getTotalPages()) {
                totalPages = aipsPage.getTotalPages();
            }
            aipsPage.forEach(aip -> {
                // Mark SIP and AIP deleted
                // Send events for files deletion
                sipService.scheduleDeletion(aip.getSip(), deletionRequest.getDeletionMode(),
                                            deletionRequest.getDeletePhysicalFiles());
            });
            advanceCompletion();
        } while (aipsPage.hasNext());

        requestService.cleanRequestJob(deletionRequest);
    }

    @Override
    public int getCompletionCount() {
        // Do not return 0 value. Completion must be a positive integer.
        return totalPages > 0 ? totalPages : 1;
    }

}
