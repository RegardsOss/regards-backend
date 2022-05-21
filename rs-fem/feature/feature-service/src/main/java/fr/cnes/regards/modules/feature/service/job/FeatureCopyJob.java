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

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.feature.dao.IFeatureCopyRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.FeatureCopyRequest;
import fr.cnes.regards.modules.feature.service.IFeatureCopyService;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Copy a feature in an other store
 *
 * @author Kevin Marchois
 */
public class FeatureCopyJob extends AbstractFeatureJob {

    private List<FeatureCopyRequest> featureCopyRequests;

    @Autowired
    private IFeatureCopyRequestRepository featureCopyRequestRepo;

    @Autowired
    private IFeatureCopyService featureCopyService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        Type type = new TypeToken<Set<Long>>() {

        }.getType();
        featureCopyRequests = this.featureCopyRequestRepo.findAllById(getValue(parameters, IDS_PARAMETER, type));
    }

    @Override
    public void run() {
        logger.info("[{}] Feature copy job starts", jobInfoId);
        long start = System.currentTimeMillis();
        this.featureCopyService.processRequests(featureCopyRequests, this);
        logger.info("[{}]{} Copy request(s) processed in {} ms",
                    jobInfoId,
                    INFO_TAB,
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getCompletionCount() {
        return featureCopyRequests.size();
    }
}
