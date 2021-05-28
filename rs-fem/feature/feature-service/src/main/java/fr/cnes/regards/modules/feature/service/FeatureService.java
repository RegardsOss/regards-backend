/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.feature.dao.FeatureEntitySpecification;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.FeaturesSelectionDTO;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.job.PublishFeatureNotificationJob;
import fr.cnes.regards.modules.feature.service.job.ScheduleFeatureDeletionJobsJob;

/**
 *  Serive to create {@link DataObjectFeature} from {@link FeatureEntity}
 *  @author Kevin Marchois
 *  @author Sébastien Binda
 *
 */
@Service
@MultitenantTransactional
public class FeatureService implements IFeatureService {

    @Autowired
    private IFeatureEntityRepository featureRepo;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Override
    public Page<FeatureEntityDto> findAll(FeaturesSelectionDTO selection, Pageable page) {
        Page<FeatureEntity> entities = featureRepo
                .findAll(FeatureEntitySpecification.searchAllByFilters(selection, page), page);
        List<FeatureEntityDto> elements = entities.stream()
                .map(entity -> initDataObjectFeature(entity, selection.getFilters().isFull()))
                .collect(Collectors.toList());
        return new PageImpl<FeatureEntityDto>(elements, page, entities.getTotalElements());
    }

    @Override
    public FeatureEntityDto findOne(FeatureUniformResourceName urn) {
        return initDataObjectFeature(featureRepo.findByUrn(urn), true);
    }

    private FeatureEntityDto initDataObjectFeature(FeatureEntity entity, boolean addFeatureContent) {
        FeatureEntityDto dto = new FeatureEntityDto();
        dto.setSession(entity.getSession());
        dto.setSource(entity.getSessionOwner());
        dto.setProviderId(entity.getProviderId());
        dto.setVersion(entity.getVersion());
        dto.setLastUpdate(entity.getLastUpdate());
        dto.setUrn(entity.getUrn());
        dto.setId(entity.getId());
        if (addFeatureContent) {
            dto.setFeature(entity.getFeature());
        }
        return dto;
    }

    @Override
    public JobInfo scheduleNotificationsJob(FeaturesSelectionDTO selection) {
        // Schedule job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(PublishFeatureNotificationJob.SELECTION_PARAMETER, selection));
        jobParameters.add(new JobParameter(PublishFeatureNotificationJob.OWNER_PARAMETER, authResolver.getUser()));
        // the job priority will be set according the priority of the first request to schedule
        JobInfo jobInfo = new JobInfo(false, PriorityLevel.HIGH.getPriorityLevel(), jobParameters,
                authResolver.getUser(), PublishFeatureNotificationJob.class.getName());
        return jobInfoService.createAsQueued(jobInfo);
    }

    @Override
    public JobInfo scheduleDeletionJob(FeaturesSelectionDTO selection) {
        // Schedule job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(ScheduleFeatureDeletionJobsJob.SELECTION_PARAMETER, selection));
        jobParameters.add(new JobParameter(ScheduleFeatureDeletionJobsJob.OWNER_PARAMETER, authResolver.getUser()));
        // the job priority will be set according the priority of the first request to schedule
        JobInfo jobInfo = new JobInfo(false, PriorityLevel.HIGH.getPriorityLevel(), jobParameters,
                authResolver.getUser(), ScheduleFeatureDeletionJobsJob.class.getName());
        return jobInfoService.createAsQueued(jobInfo);
    }

}