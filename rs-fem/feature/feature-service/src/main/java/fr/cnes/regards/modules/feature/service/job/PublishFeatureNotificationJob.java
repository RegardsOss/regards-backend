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
package fr.cnes.regards.modules.feature.service.job;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.FeaturesSelectionDTO;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.IFeatureService;

/**
 * Job to publis {@link FeatureNotificationRequestEvent}s for each {@link FeatureEntityDto} matching  search parameters
 *
 * @author Sébastien Binda
 *
 */
public class PublishFeatureNotificationJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishFeatureNotificationJob.class);

    public static final String SELECTION_PARAMETER = "selection";

    public static final String OWNER_PARAMETER = "owner";

    private FeaturesSelectionDTO selection;

    private String owner;

    @Autowired
    private IFeatureService featureService;

    @Autowired
    private IPublisher publisher;

    @Value("${regards.feature.notify.notification.job.size:1000}")
    private int pageSize;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        selection = getValue(parameters, SELECTION_PARAMETER);
        owner = getValue(parameters, OWNER_PARAMETER);
    }

    @Override
    public void run() {
        Pageable page = PageRequest.of(0, pageSize);
        Page<FeatureEntityDto> results = null;
        long totalElementCheck = 0;
        boolean firstPass = true;
        do {
            // Search features to notify
            results = featureService.findAll(selection, page);
            if (firstPass) {
                totalElementCheck = results.getTotalElements();
                LOGGER.info("Starting scheduling {} feature notification requests.", totalElementCheck);
                firstPass = false;
            }
            // Scheduling page deletion job
            publishNotificationEvents(results.map(f -> f.getFeature().getUrn()).toList());
            LOGGER.info("Scheduling job for {} feature notification requests (remaining {}).",
                        results.getNumberOfElements(), totalElementCheck - results.getNumberOfElements());
            page = page.next();
        } while ((results != null) && results.hasNext());
    }

    /**
     * @param ids
     */
    private void publishNotificationEvents(Collection<FeatureUniformResourceName> featureUrns) {
        List<FeatureNotificationRequestEvent> events = Lists.newArrayList();
        for (FeatureUniformResourceName urn : featureUrns) {
            FeatureNotificationRequestEvent event = FeatureNotificationRequestEvent.build(owner, urn,
                                                                                          PriorityLevel.HIGH);
            events.add(event);
        }
        publisher.publish(events);
    }

}
