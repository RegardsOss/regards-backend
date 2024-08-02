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
package fr.cnes.regards.modules.feature.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.feature.dao.FeatureSimpleEntitySpecificationBuilder;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityWithDisseminationRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureSimpleEntityRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.FeatureSimpleEntity;
import fr.cnes.regards.modules.feature.domain.RecipientsSearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.job.PublishFeatureNotificationJob;
import fr.cnes.regards.modules.feature.service.job.ScheduleFeatureDeletionJobsJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to create {@link DataObjectFeature} from {@link FeatureEntity}
 *
 * @author Kevin Marchois
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FeatureService implements IFeatureService {

    protected static final Logger LOGGER = LoggerFactory.getLogger(FeatureService.class);

    private final IFeatureSimpleEntityRepository featureSimpleEntityRepository;

    private final IFeatureEntityWithDisseminationRepository featureWithDisseminationRepo;

    private final IAuthenticationResolver authResolver;

    private final IJobInfoService jobInfoService;

    public FeatureService(IFeatureSimpleEntityRepository featureSimpleEntityRepository,
                          IFeatureEntityWithDisseminationRepository featureWithDisseminationRepo,
                          IAuthenticationResolver authResolver,
                          IJobInfoService jobInfoService) {
        this.featureSimpleEntityRepository = featureSimpleEntityRepository;
        this.featureWithDisseminationRepo = featureWithDisseminationRepo;
        this.authResolver = authResolver;
        this.jobInfoService = jobInfoService;
    }

    /**
     * Method is annotated with transaction Isolation.REPEATABLE_READ to avoid changes of entities (due to
     * concurrent updates) between the two select of the same entities in database.
     * <p>
     * Use Case :
     * Thread 1 : select entities
     * Thread 2 : Update same entities
     * Thread 1 : select entities : with isolation REPEATABLE_READ, results are the same as the first select ( no
     * effect of the updates).
     * <p>
     * Multiple select are mandatory to avoid in memory pagination.
     * This choice is made due to SWOT issues during FEM datasource crawling.
     * <p>
     * All entities are requested by page with a criterion on lastUpdate. Without this if the last update is updated
     * between the two select, this find return entities with lastUpdate which do not match the given filters.
     */
    @Override
    @MultitenantTransactional(isolation = Isolation.REPEATABLE_READ)
    public Page<FeatureEntityDto> findAll(SearchFeatureSimpleEntityParameters filters, Pageable pageable) {
        // Workaround to avoid in-memory pagination with specification
        // 1. use simple entities with specification + pagination to get 1 page
        // 2. fetch full entities for objects in this page
        Page<FeatureSimpleEntity> featureSimpleEntities = featureSimpleEntityRepository.findAll(new FeatureSimpleEntitySpecificationBuilder().withParameters(
            filters).build(), pageable);
        List<FeatureEntity> featureEntities = featureWithDisseminationRepo.findByIdIn(featureSimpleEntities.stream()
                                                                                                           .map(
                                                                                                               FeatureSimpleEntity::getId)
                                                                                                           .collect(
                                                                                                               Collectors.toSet()),
                                                                                      featureSimpleEntities.getSort());

        List<FeatureEntityDto> featureEntityDtos = featureEntities.stream().map(entity -> entity.toDto(true)).toList();
        return new PageImpl<>(featureEntityDtos, pageable, featureSimpleEntities.getTotalElements());
    }

    @Override
    public FeatureEntityDto findOne(FeatureUniformResourceName urn) {
        return featureWithDisseminationRepo.findByUrn(urn).toDto(true);
    }

    @Override
    public JobInfo scheduleNotificationsJob(RecipientsSearchFeatureSimpleEntityParameters selection) {
        // Schedule job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(PublishFeatureNotificationJob.SELECTION_PARAMETER,
                                           selection.getSearchParameters()));
        jobParameters.add(new JobParameter(PublishFeatureNotificationJob.OWNER_PARAMETER, authResolver.getUser()));
        jobParameters.add(new JobParameter(PublishFeatureNotificationJob.RECIPIENTS_PARAMETER,
                                           selection.getRecipientIds()));
        // the job priority will be set according the priority of the first request to schedule
        JobInfo jobInfo = new JobInfo(false,
                                      PriorityLevel.HIGH.getPriorityLevel(),
                                      jobParameters,
                                      authResolver.getUser(),
                                      PublishFeatureNotificationJob.class.getName());

        return jobInfoService.createAsQueued(jobInfo);
    }

    @Override
    public JobInfo scheduleDeletionJob(SearchFeatureSimpleEntityParameters selection) {
        // Schedule job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(ScheduleFeatureDeletionJobsJob.SELECTION_PARAMETER, selection));
        jobParameters.add(new JobParameter(ScheduleFeatureDeletionJobsJob.OWNER_PARAMETER, authResolver.getUser()));
        // the job priority will be set according the priority of the first request to schedule
        JobInfo jobInfo = new JobInfo(false,
                                      PriorityLevel.HIGH.getPriorityLevel(),
                                      jobParameters,
                                      authResolver.getUser(),
                                      ScheduleFeatureDeletionJobsJob.class.getName());
        return jobInfoService.createAsQueued(jobInfo);
    }

}
