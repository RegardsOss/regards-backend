/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.reflect.TypeToken;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.service.FeatureMetrics;
import fr.cnes.regards.modules.feature.service.FeatureMetrics.FeatureCreationState;
import fr.cnes.regards.modules.feature.service.IFeatureCreationService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 *
 * Store a new feature
 *
 * @author Marc SORDI
 *
 */
public class FeatureCreationJob extends AbstractJob<Void> {

    public static final String IDS_PARAMETER = "ids";

    private List<FeatureCreationRequest> featureCreationRequests;

    @Autowired
    private IFeatureCreationRequestRepository featureCreationRequestRepo;

    @Autowired
    private IFeatureCreationService featureService;

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private FeatureMetrics metrics;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        Type type = new TypeToken<Set<Long>>() {

        }.getType();
        featureCreationRequests = this.featureCreationRequestRepo
                .findAllById(getValue(parameters, IDS_PARAMETER, type));
    }

    @Override
    public void run() {
        logger.info("[{}] Feature creation job starts", jobInfoId);
        long start = System.currentTimeMillis();
        Timer.Sample sample = Timer.start(registry);
        Set<FeatureEntity> created = featureService.processRequests(featureCreationRequests, this);
        sample.stop(Timer.builder(this.getClass().getName()).tag("job", "run").register(registry));
        created.forEach(e -> metrics.count(e.getProviderId(), e.getUrn(), FeatureCreationState.FEATURE_CREATED));
        logger.info("[{}]{}{} creation request(s) processed in {} ms", jobInfoId, INFO_TAB,
                    featureCreationRequests.size(), System.currentTimeMillis() - start);
    }

    @Override
    public int getCompletionCount() {
        return featureCreationRequests.size();
    }
}
