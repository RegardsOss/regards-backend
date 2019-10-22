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
package fr.cnes.regards.modules.ingest.service.flow;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.service.job.AIPUpdateRunnerJob;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * This component scans the AIPUpdateRepo and regroups tasks by aip to update
 *
 * @author Leo Mieulet
 */
@Component
@MultitenantTransactional
public class AIPUpdateFlowHandler {

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private AIPUpdateFlowHandler self;

    @Autowired
    private JobInfoService jobInfoService;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Limit number of AIPs to retrieve in one page.
     */
    @Value("${regards.ingest.aips.scan.iteration-limit:100}")
    private Integer updateRequestIterationLimit;

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(fixedDelayString = "${regards.ingest.aip.update.bulk.delay:10000}")
    protected void handleQueue() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                // Call transactional proxy
                self.scheduleUpdateJobs();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    public void scheduleUpdateJobs() {
        JobInfo jobInfo = getUpdateJob();
        if (jobInfo != null) {
            jobInfo.updateStatus(JobStatus.QUEUED);
            jobInfoService.save(jobInfo);
        }
    }

    public JobInfo getUpdateJob() {
        JobInfo jobInfo = null;
        Pageable pageRequest = PageRequest.of(0, updateRequestIterationLimit, Sort.Direction.ASC, "id");
        // Fetch the first list of update request to handle
        Page<AIPUpdateRequest> waitingRequest = aipUpdateRequestRepository.findWaitingRequest(pageRequest);
        if (!waitingRequest.isEmpty()) {
            // Fetch all update request linked to same aips
            List<AIPUpdateRequest> content = waitingRequest.getContent();
            List<AIPUpdateRequest> request = new ArrayList<>(content);
            List<Long> aipIds = content.stream().map(wr -> wr.getAip().getId()).collect(Collectors.toList());
            List<AIPUpdateRequest> linkedTasks = aipUpdateRequestRepository.findAllByAipIdIn(aipIds);
            request.addAll(linkedTasks);
            request.forEach(task -> task.setState(InternalRequestStep.RUNNING));
            aipUpdateRequestRepository.saveAll(request);

            // Make a list of content ids
            List<Long> requestIds = request.stream().map(AIPUpdateRequest::getId).collect(Collectors.toList());

            // Schedule deletion job
            Set<JobParameter> jobParameters = Sets.newHashSet();
            jobParameters.add(new JobParameter(AIPUpdateRunnerJob.UPDATE_REQUEST_IDS, requestIds));
            jobInfo = new JobInfo(false, IngestJobPriority.UPDATE_AIP_RUNNER_PRIORITY.getPriority(),
                    jobParameters, null, AIPUpdateRunnerJob.class.getName());
            jobInfoService.createAsPending(jobInfo);
        }
        return jobInfo;
    }
}
