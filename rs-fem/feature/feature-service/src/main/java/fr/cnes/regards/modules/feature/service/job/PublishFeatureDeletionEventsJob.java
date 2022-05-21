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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Job to publis {@link FeatureDeletionRequestEvent}s for each {@link FeatureEntityDto} urn
 *
 * @author Sébastien Binda
 */
public class PublishFeatureDeletionEventsJob extends AbstractJob<Void> {

    public static final String URNS_PARAMETER = "urns";

    public static final String OWNER_PARAMETER = "owner";

    private Set<String> urns;

    private String owner;

    @Autowired
    private IPublisher publisher;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        urns = getValue(parameters, URNS_PARAMETER);
        owner = getValue(parameters, OWNER_PARAMETER);
    }

    @Override
    public void run() {
        // Prepare features
        List<FeatureUniformResourceName> features = new ArrayList<>();
        for (String urn : urns) {
            try {
                features.add(FeatureUniformResourceName.fromString(urn));
            } catch (IllegalArgumentException e) {
                logger.error(
                    "Error trying to delete feature {} from FEM microservice. Feature identifier is not a valid FeatureUniformResourceName. Cause: {}",
                    urn,
                    e.getMessage());
            }
        }
        List<FeatureDeletionRequestEvent> events = Lists.newArrayList();
        for (FeatureUniformResourceName urn : features) {
            FeatureDeletionRequestEvent event = FeatureDeletionRequestEvent.build(owner, urn, PriorityLevel.HIGH);
            events.add(event);
        }
        publisher.publish(events);
        logger.info("{} feature deletion requests sended.", features.size());
    }
}
