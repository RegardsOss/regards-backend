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
package fr.cnes.regards.modules.ltamanager.service.deletion;

import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ltamanager.dao.submission.SubmissionRequestSpecificationBuilder;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.domain.submission.search.SearchSubmissionRequestParameters;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Job to delete submission request which respect search params
 * <p>
 * Input params are
 * <li>search criteria {@link SearchSubmissionRequestParameters}</li>
 * <li>schedule date which will be set in search criteria if not already set.
 * This behaviour is to avoid deletion of submission request created after the schedule request</li>
 *
 * @author Thomas GUILLOU
 **/
public class SubmissionRequestDeletionJob extends AbstractJob<Void> {

    public static final String SPECIFICATIONS_PARAM_NAME = "SPECIFICATIONS_PARAM_NAME";

    public static final String SCHEDULE_DATE = "SCHEDULE_DATE";

    public static final int JOB_PRIORITY = 10;

    private static final int DEFAULT_BATCH_SIZE = 1000;

    @Autowired
    private SubmissionDeleteService submissionDeleteService;

    @Value("${regards.ltamanager.request.deletion.batch.size:" + DEFAULT_BATCH_SIZE + "}")
    private int batchSize;

    // PARAMS
    private SearchSubmissionRequestParameters searchCriterionParam;

    private OffsetDateTime scheduleDateParam;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        searchCriterionParam = getValue(parameters, SPECIFICATIONS_PARAM_NAME);
        scheduleDateParam = getValue(parameters, SCHEDULE_DATE);

    }

    @Override
    public void run() {
        logger.debug("[{}] SubmissionRequestDeletionJob starts", jobInfoId);
        long start = System.currentTimeMillis();

        adaptCriterion(searchCriterionParam);
        Specification<SubmissionRequest> requestSpecification = new SubmissionRequestSpecificationBuilder().withParameters(
            searchCriterionParam).build();
        submissionDeleteService.deleteAll(requestSpecification);

        logger.debug("[{}] SubmissionRequestDeletionJob ended in {}ms.", jobInfoId, System.currentTimeMillis() - start);
    }

    private void adaptCriterion(SearchSubmissionRequestParameters searchCriterion) {
        if (searchCriterion.getCreationDate() == null) {
            // avoid to delete submission request created after deletion request
            searchCriterion.setCreationDate(DatesRangeRestriction.buildBefore(scheduleDateParam));
        }
        if (searchCriterion.getStatusesRestriction() == null) {
            // avoid deletion of submission request with not final state
            searchCriterion.setStatusesRestriction(new ValuesRestriction<SubmissionRequestState>().withInclude(List.of(
                SubmissionRequestState.getAllFinishedState())));
        }
    }
}
