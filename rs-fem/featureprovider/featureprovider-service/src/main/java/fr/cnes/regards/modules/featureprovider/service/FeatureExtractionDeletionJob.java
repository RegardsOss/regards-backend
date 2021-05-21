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
package fr.cnes.regards.modules.featureprovider.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Iliana Ghazali
 **/
public class FeatureExtractionDeletionJob extends AbstractJob<Void> {

    public static final String SOURCE_NAME_PARAM = "sourceName";

    public static final String SESSION_NAME_PARAM = "sessionName";

    private String sourceName;

    private String sessionName;

    @Autowired
    private FeatureExtractionDeletionService deletionService;

    public static Set<JobParameter> getParameters(String source, Optional<String> session) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(FeatureExtractionDeletionJob.SOURCE_NAME_PARAM, source));
        session.ifPresent(s -> parameters.add(new JobParameter(FeatureExtractionDeletionJob.SESSION_NAME_PARAM, s)));
        return parameters;
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        this.sourceName = getValue(parameters, SOURCE_NAME_PARAM);
        this.sessionName = (String) getOptionalValue(parameters, SESSION_NAME_PARAM).orElse(null);
    }

    @Override
    public void run() {
        logger.trace("[{}] FeatureExtractionDeletionJob starts for source {}", jobInfoId, sourceName);
        long start = System.currentTimeMillis();
        long nbDeletedRequests = deletionService.deleteFeatureExtractionRequest(sourceName, sessionName);
        logger.trace("[{}] FeatureExtractionDeletionJob ends in {} ms. {} extractionRequestsDeleted ",
                     jobInfoId, System.currentTimeMillis() - start, nbDeletedRequests);
    }

}
