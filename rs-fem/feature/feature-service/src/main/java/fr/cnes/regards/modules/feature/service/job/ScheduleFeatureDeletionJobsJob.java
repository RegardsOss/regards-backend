/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service.job;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.service.IFeatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Job to schedule one {@link PublishFeatureDeletionEventsJob} job for each page of {@link FeatureEntityDto} matching search parameters
 *
 * @author Sébastien Binda
 */
public class ScheduleFeatureDeletionJobsJob extends AbstractJob<Void> {

    public static final String SELECTION_PARAMETER = "selection";

    public static final String OWNER_PARAMETER = "owner";

    private SearchFeatureSimpleEntityParameters selection;

    private String owner;

    @Autowired
    private IFeatureService featureService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Value("${regards.feature.deletion.notification.job.size:1000}")
    private int pageSize;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        selection = getValue(parameters, SELECTION_PARAMETER);
        owner = getValue(parameters, OWNER_PARAMETER);
    }

    @Override
    public void run() {
        Pageable page = PageRequest.of(0, pageSize);
        Page<FeatureEntityDto> results = null;
        long totalElementCheck = 0;
        boolean firstPass = true;
        do {
            // Search features to delete
            results = featureService.findAll(selection, page);
            if (!results.isEmpty()) {
                if (firstPass) {
                    totalElementCheck = results.getTotalElements();
                    logger.info("Starting scheduling {} feature deletion requests.", totalElementCheck);
                    firstPass = false;
                }
                // Prepare urns
                Set<String> ids = new HashSet<>();
                for (FeatureEntityDto feature : results.getContent()) {
                    ids.add(feature.getFeature().getUrn().toString());
                    totalElementCheck--;
                }
                // Scheduling page deletion job
                schedulePageDeletion(ids);
                logger.info("Scheduling job for {} feature deletion requests (remaining {}).",
                            ids.size(),
                            totalElementCheck);
                page = page.next();
            }
        } while ((results != null) && results.hasNext());
    }

    /**
     * Schedule {@link PublishFeatureDeletionEventsJob} with the given request ids as parameter
     */
    private JobInfo schedulePageDeletion(Set<String> ids) {
        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(PublishFeatureDeletionEventsJob.URNS_PARAMETER, ids));
        jobParameters.add(new JobParameter(PublishFeatureDeletionEventsJob.OWNER_PARAMETER, owner));
        JobInfo jobInfo = new JobInfo(false, 0, jobParameters, owner, PublishFeatureDeletionEventsJob.class.getName());
        return jobInfoService.createAsQueued(jobInfo);
    }

}
