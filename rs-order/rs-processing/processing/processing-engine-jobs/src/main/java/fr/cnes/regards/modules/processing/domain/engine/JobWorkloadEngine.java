/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.ProcessingConstants;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;

/**
 * This class defines a worload engine based on REGARDS Jobs mechanism.
 *
 * In order to launch an execution, the engine creation a {@link JobInfo} referencing
 * a {@link LaunchExecutionJob}. The actual execution (calling the process' executable on the
 * execution parameters) will be done by this job.
 *
 * @author gandrieu
 */
@Component
public class JobWorkloadEngine implements IWorkloadEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobWorkloadEngine.class);

    private final IJobInfoService jobInfoService;

    private final IWorkloadEngineRepository engineRepo;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    public JobWorkloadEngine(
            IJobInfoService jobInfoService,
            IWorkloadEngineRepository engineRepo,
            IRuntimeTenantResolver runtimeTenantResolver
    ) {
        this.jobInfoService = jobInfoService;
        this.engineRepo = engineRepo;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public String name() {
        return ProcessingConstants.Engines.JOBS;
    }

    @Override
    @PostConstruct
    public void selfRegisterInRepo() {
        engineRepo.register(this);
    }

    @Override
    public Mono<PExecution> run(ExecutionContext context) {
        return Mono.fromCallable(() -> {
            try {
                JobInfo jobInfo = new JobInfo(false, 0,
                        List.of(new JobParameter(LaunchExecutionJob.EXEC_ID_PARAM, context.getExec().getId())).toJavaSet(),
                        context.getBatch().getUser(), LaunchExecutionJob.class.getName());

                jobInfo.setExpirationDate(nowUtc().plus(context.getExec().getExpectedDuration()));
                String tenant = context.getExec().getTenant();
                runtimeTenantResolver.forceTenant(tenant);
                JobInfo pendingJob = jobInfoService.createAsQueued(jobInfo);

                LOGGER.info("batch={} exec={} - Job created with ID {}", context.getBatch().getId(),
                        context.getExec().getId(), pendingJob.getId());

                return context.getExec();
            }
            finally {
                runtimeTenantResolver.clearTenant();
            }
        });

    }
}
