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
package fr.cnes.regards.modules.feature.service.job;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.feature.dao.FeatureSimpleEntitySpecificationBuilder;
import fr.cnes.regards.modules.feature.domain.FeatureSimpleEntity;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.dto.FeatureIdUrnDto;
import fr.cnes.regards.modules.feature.service.IFeatureService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Job to schedule one {@link PublishFeatureDeletionEventsJob} job for each page of {@link FeatureIdUrnDto} matching search parameters
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
        Page<FeatureIdUrnDto> results = null;
        long remainingFeaturesToDelete = 0;
        boolean firstPass = true;

        // Mandatory sort by id, useful for algorithm in order to load all features to delete
        Pageable page = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "id"));

        AtomicReference<Long> lastProcessedId = new AtomicReference<>(null);

        Specification<FeatureSimpleEntity> spec = new FeatureSimpleEntitySpecificationBuilder().withParameters(selection)
                                                                                               .build();
        do {
            // Hack in order to avoid lost or to forget data during the pagination because some feature can delete in
            // parallel, so add identifier in request in order to load pageSize elements always > last
            // processed identifier
            Specification<FeatureSimpleEntity> paginatedSpec = (root, query, cb) -> {
                Predicate predicate = spec != null ? spec.toPredicate(root, query, cb) : cb.conjunction();
                if (lastProcessedId.get() != null) {
                    Predicate idPredicate = cb.greaterThan(root.get("id"), lastProcessedId.get());
                    predicate = cb.and(predicate, idPredicate);
                }
                return predicate;
            };

            // Prepare URNs of feature to delete
            results = featureService.findAll(paginatedSpec, page);
            if (!results.isEmpty()) {
                if (firstPass) {
                    remainingFeaturesToDelete = results.getTotalElements();
                    logger.info("Starting scheduling job for {} feature deletion requests.", remainingFeaturesToDelete);
                    firstPass = false;
                }

                // Create URNs of feature
                Set<String> urns = results.getContent()
                                          .stream()
                                          .map(feature -> feature.urn().toString())
                                          .collect(Collectors.toSet());
                remainingFeaturesToDelete -= urns.size();

                // Set the last processed id for next iteration
                lastProcessedId.set(results.getContent().get(results.getContent().size() - 1).id());

                // Scheduling page of urns for each deletion job
                schedulePageDeletion(urns);

                logger.info("Scheduling job for {} feature deletion requests (remaining {}).",
                            urns.size(),
                            remainingFeaturesToDelete);
            }
        } while (results.hasNext());
    }

    /**
     * Schedule {@link PublishFeatureDeletionEventsJob} with the given urns of feature
     */
    private void schedulePageDeletion(Set<String> urns) {
        Set<JobParameter> jobParameters = new HashSet<>();
        jobParameters.add(new JobParameter(PublishFeatureDeletionEventsJob.URNS_PARAMETER, urns));
        jobParameters.add(new JobParameter(PublishFeatureDeletionEventsJob.OWNER_PARAMETER, owner));

        jobInfoService.createAsQueued(new JobInfo(false,
                                                  0,
                                                  jobParameters,
                                                  owner,
                                                  PublishFeatureDeletionEventsJob.class.getName()));
    }
}
