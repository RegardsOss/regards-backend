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
package fr.cnes.regards.modules.ltamanager.service.deletion;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMode;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.domain.submission.search.SearchSubmissionRequestParameters;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas GUILLOU
 **/
@Service
@MultitenantTransactional
public class SubmissionDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionDeleteService.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    @Value("${regards.ltamanager.request.deletion.batch.size:" + DEFAULT_BATCH_SIZE + "}")
    private int batchSize;

    private final IAuthenticationResolver authResolver;

    private final IJobInfoService jobInfoService;

    private final ISubmissionRequestRepository submissionRequestRepository;

    public SubmissionDeleteService(IAuthenticationResolver authResolver,
                                   IJobInfoService jobInfoService,
                                   ISubmissionRequestRepository submissionRequestRepository) {
        this.authResolver = authResolver;
        this.jobInfoService = jobInfoService;
        this.submissionRequestRepository = submissionRequestRepository;
    }

    /**
     * Schedule submission request deletion job if search criterion respect few conditions :
     * <li>there is no other deletion job running or waiting</li>
     * <li>status criteria must be in include mode</li>
     * <li>status criteria must be a final state</li>
     *
     * @throws ModuleException if conditions are not respected
     */
    public JobInfo scheduleRequestDeletionJob(SearchSubmissionRequestParameters searchCriterion)
        throws ModuleException {
        checkSearchParametersValidOrThrow(searchCriterion);

        // Check if a job of deletion already exists
        if (jobInfoService.retrieveJobsCount(SubmissionRequestDeletionJob.class.getName(),
                                             JobStatus.getAllNotFinishedStatus()) > 0) {
            throw new ModuleException(
                "Cannot schedule request deletion process : another deletion job is already running");
        }

        Set<JobParameter> jobParameters = Sets.newHashSet(new JobParameter(SubmissionRequestDeletionJob.SPECIFICATIONS_PARAM_NAME,
                                                                           searchCriterion),
                                                          new JobParameter(SubmissionRequestDeletionJob.SCHEDULE_DATE,
                                                                           OffsetDateTime.now()));
        JobInfo jobInfo = new JobInfo(false,
                                      SubmissionRequestDeletionJob.JOB_PRIORITY,
                                      jobParameters,
                                      authResolver.getUser(),
                                      SubmissionRequestDeletionJob.class.getName());
        jobInfo = jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("Schedule {} job with id {}", SubmissionRequestDeletionJob.class.getName(), jobInfo.getId());
        return jobInfo;
    }

    private void checkSearchParametersValidOrThrow(SearchSubmissionRequestParameters searchCriterion) {
        if (searchCriterion.getStatusesRestriction() == null) {
            // nothing to validate
            return;
        }
        // check only status criteria :
        // only include mode is allowed
        // only request with final status are authorized to be deleted
        if (searchCriterion.getStatusesRestriction().getMode() == ValuesRestrictionMode.INCLUDE) {
            boolean allStatesAreFinal = searchCriterion.getStatusesRestriction()
                                                       .getValues()
                                                       .stream()
                                                       .allMatch(SubmissionRequestState::isFinalState);
            if (!allStatesAreFinal) {
                throw new IllegalArgumentException("Only final status ("
                                                   + Arrays.stream(SubmissionRequestState.getAllFinishedState())
                                                           .map(SubmissionRequestState::toString)
                                                           .collect(Collectors.joining(", "))
                                                   + ") are allowed in status search criterion.");
            }
        } else {
            throw new IllegalArgumentException("Only include mode is allowed in status search criterion.");
        }
    }

    public void deleteAll(Specification<SubmissionRequest> requestSpecification) {
        submissionRequestRepository.delete(requestSpecification);
    }

}
