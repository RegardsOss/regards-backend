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


import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateTask;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * This job creates {@link AIPUpdateTask} task to update. It scans AIP and create for each modification a task
 *
 * @author Léo Mieulet
 */
public class AIPUpdateScannerJob extends AbstractJob<Void> {

    public static final String CRITERIA = "CRITERIA";

    public static final String TASK = "TASK";

    private SearchAIPsParameters searchParameters;

    private AIPUpdateTask updateTask;

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
        // Retrieve search parameters
        searchParameters = getValue(parameters, CRITERIA);
        // Retrieve search parameters
        updateTask = getValue(parameters, CRITERIA);
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
            aipsPage = aipRepository.search(searchParameters, pageRequest);
            // Save number of pages to publish job advancement
            if (totalPages < aipsPage.getTotalPages()) {
                totalPages = aipsPage.getTotalPages();
            }
            aipsPage.forEach(aip -> {
                AIPUpdateRequest aipUpdateRequest = AIPUpdateRequest.build(aip, updateTask);
                aipUpdateRequestRepository.save(aipUpdateRequest);
            });
            isFirstPage = false;
            advanceCompletion();
            pageRequest.next();
        } while (aipsPage.hasNext());
    }

    @Override
    public int getCompletionCount() {
        return totalPages;
    }

}