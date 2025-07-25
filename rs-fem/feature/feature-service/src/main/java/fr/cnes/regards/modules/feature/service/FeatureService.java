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
import fr.cnes.regards.modules.feature.dao.*;
import fr.cnes.regards.modules.feature.domain.*;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.FeatureEntityRawDto;
import fr.cnes.regards.modules.feature.dto.FeatureIdUrnDto;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.job.PublishFeatureNotificationJob;
import fr.cnes.regards.modules.feature.service.job.ScheduleFeatureDeletionJobsJob;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    private final IFeatureSimpleEntityCustomRepository featureSimpleEntityCustomRepository;

    private final IFeatureEntityWithDisseminationRepository featureWithDisseminationRepo;

    private final IFeatureSimpleRawEntityRepository featureSimpleRawEntityRepository;

    private final IAuthenticationResolver authResolver;

    private final IJobInfoService jobInfoService;

    public FeatureService(IFeatureSimpleEntityRepository featureSimpleEntityRepository,
                          IFeatureEntityWithDisseminationRepository featureWithDisseminationRepo,
                          IFeatureSimpleRawEntityRepository featureSimpleRawEntityRepository,
                          IAuthenticationResolver authResolver,
                          IJobInfoService jobInfoService,
                          IFeatureSimpleEntityCustomRepository featureSimpleEntityCustomRepository) {
        this.featureSimpleEntityRepository = featureSimpleEntityRepository;
        this.featureWithDisseminationRepo = featureWithDisseminationRepo;
        this.featureSimpleRawEntityRepository = featureSimpleRawEntityRepository;
        this.authResolver = authResolver;
        this.jobInfoService = jobInfoService;
        this.featureSimpleEntityCustomRepository = featureSimpleEntityCustomRepository;
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
    @Timed(value = "feature_find_all", description = "Durée d'exécution de FeatureService#findAll")
    // Prevent the entity being updated between the two successive find calls
    public Page<FeatureEntityDto> findAll(SearchFeatureSimpleEntityParameters filters, Pageable pageable) {
        LOGGER.debug("Search features with filters : {}", filters);
        long start = System.currentTimeMillis();
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
        LOGGER.debug("Search features with filters complete in {} ms", System.currentTimeMillis() - start);
        return new PageImpl<>(featureEntityDtos, pageable, featureSimpleEntities.getTotalElements());
    }

    /**
     * Find all features entities.
     *
     * @return {@link Page} of {@link FeatureEntityRawDto} with the feature field serialized as a JSON String.
     */
    @Override
    @MultitenantTransactional(readOnly = true)
    public Page<FeatureIdUrnDto> findAll(Specification<FeatureSimpleEntity> filters, Pageable pageable) {
        LOGGER.debug("Filters feature deletion: {}", filters);
        long start = System.currentTimeMillis();

        // With Spring Boot 3.5, the following code should work and just load the fields required by the projection
        // (in Spring Boot 3.3, it works but it does the projection on the java side)
        //        Page<FeatureIdUrnDto> featureIdUrnDto = featureSimpleEntityRepository.findBy(filters,
        //                                                                                           q -> q.as(FeatureIdUrnDto.class)
        //                                                                                                 .page(pageable));
        // So in spring boot 3.3, use a custom repository with a specialized DTO:
        Page<FeatureIdUrnDto> featureIdUrnDto = featureSimpleEntityCustomRepository.findAll(filters, pageable);

        LOGGER.debug("Filters feature deletion registered in {} ms", System.currentTimeMillis() - start);
        return featureIdUrnDto;
    }

    /**
     * Find all features entities without deserializing the feature.
     *
     * @return {@link Page} of {@link FeatureEntityRawDto} with the feature field serialized as a JSON String.
     */
    @Override
    @Timed(value = "feature_find_all_raw", description = "Durée d'exécution de FeatureService#findAllRaw")
    public Page<FeatureEntityRawDto> findAllRaw(SearchFeatureSimpleEntityParameters filters, Pageable pageable) {
        long startRetrieving = System.currentTimeMillis();
        Page<FeatureSimpleRawEntity> featureSimpleRawEntities = featureSimpleRawEntityRepository.findAll(new FeatureSimpleRawEntitySpecificationBuilder().withParameters(
            filters).build(), pageable);
        LOGGER.debug("Retrieving {} entities took {} ms",
                     featureSimpleRawEntities.getContent().size(),
                     System.currentTimeMillis() - startRetrieving);
        long startMapping = System.currentTimeMillis();
        List<FeatureEntityRawDto> dtoWithJsonObject = featureSimpleRawEntities.getContent()
                                                                              .stream()
                                                                              .map(FeatureSimpleRawEntity::toDto)
                                                                              .toList();
        LOGGER.debug("String to JsonObject mapping took {} ms for {} entities",
                     System.currentTimeMillis() - startMapping,
                     dtoWithJsonObject.size());
        return new PageImpl<>(dtoWithJsonObject, pageable, featureSimpleRawEntities.getTotalElements());
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
        return jobInfoService.createAsQueued(new JobInfo(false,
                                                         PriorityLevel.HIGH.getPriorityLevel(),
                                                         jobParameters,
                                                         authResolver.getUser(),
                                                         PublishFeatureNotificationJob.class.getName()));
    }

    @Override
    public JobInfo scheduleDeletionJob(SearchFeatureSimpleEntityParameters selection) {
        // Schedule job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(ScheduleFeatureDeletionJobsJob.SELECTION_PARAMETER, selection));
        jobParameters.add(new JobParameter(ScheduleFeatureDeletionJobsJob.OWNER_PARAMETER, authResolver.getUser()));

        // the job priority will be set according the priority of the first request to schedule
        return jobInfoService.createAsQueued(new JobInfo(false,
                                                         PriorityLevel.HIGH.getPriorityLevel(),
                                                         jobParameters,
                                                         authResolver.getUser(),
                                                         ScheduleFeatureDeletionJobsJob.class.getName()));
    }
}
