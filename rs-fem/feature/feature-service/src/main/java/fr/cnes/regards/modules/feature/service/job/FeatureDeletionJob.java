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

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.service.IFeatureDeletionService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This job is used to delete a list of feature {@link FeatureDeletionRequest}
 *
 * @author kevin
 */
public class FeatureDeletionJob extends AbstractFeatureJob {

    private List<FeatureDeletionRequest> featureDeletionRequests;

    @Autowired
    private IFeatureDeletionService featureDeletionService;

    @Autowired
    private MeterRegistry registry;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        Set<Long> featureDeletionRequestIds = getValue(parameters, IDS_PARAMETER, new TypeToken<Set<Long>>() {

        }.getType());
        featureDeletionRequests = featureDeletionService.findAllByIds(featureDeletionRequestIds);
    }

    @Override
    public void run() {
        Timer timer = Timer.builder(this.getClass().getName()).tag("job", "run").register(registry);
        logger.info("[{}] Feature deletion job starts. Handle {} feature deletion requests.",
                    jobInfoId,
                    featureDeletionRequests.size());
        long start = System.currentTimeMillis();

        timer.record(() -> featureDeletionService.processRequests(featureDeletionRequests, this));
        
        logger.info("[{}]{}{} Feature deletion request(s) processed in {}ms",
                    jobInfoId,
                    INFO_TAB,
                    featureDeletionRequests.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getCompletionCount() {
        return featureDeletionRequests.size();
    }
}
