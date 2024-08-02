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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.IFeatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Job to publish {@link FeatureNotificationRequestEvent}s for each {@link FeatureEntityDto} matching search parameters
 *
 * @author Sébastien Binda
 */
public class PublishFeatureNotificationJob extends AbstractJob<Void> {

    public static final String SELECTION_PARAMETER = "selection";

    public static final String OWNER_PARAMETER = "owner";

    public static final String RECIPIENTS_PARAMETER = "recipients";

    private SearchFeatureSimpleEntityParameters selection;

    private String owner;

    /**
     * List of recipients(business identifiers of plugin configurations
     * {@link fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration}) for the direct
     * notification
     */
    private Set<String> recipients;

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
        recipients = getValue(parameters, RECIPIENTS_PARAMETER);
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        Pageable page = PageRequest.of(0, pageSize);
        Page<FeatureEntityDto> results = null;
        long totalElementCheck = 0;
        boolean firstPass = true;

        do {
            // Search features to notify
            results = featureService.findAll(selection, page);
            if (firstPass) {
                totalElementCheck = results.getTotalElements();
                logger.info("[PUBLISH FEATURE NOTIFICATION JOB] Starting scheduling {} feature notification request"
                            + "(s).", totalElementCheck);
                firstPass = false;
            }
            // Publish notification requests of features with the list of recipients for the direct notification
            publishFeatureNotificationRequestEvents(results.map(f -> f.getFeature().getUrn()).toList(), recipients);

            logger.debug("[PUBLISH FEATURE NOTIFICATION JOB] Publish job for {} feature notification requests "
                         + "(remaining {}).",
                         results.getNumberOfElements(),
                         totalElementCheck - results.getNumberOfElements());
            page = page.next();
        } while (results != null && results.hasNext() && !Thread.interrupted());
        logger.debug("[PUBLISH FEATURE NOTIFICATION JOB] {} feature notification request(s) in {}ms",
                     totalElementCheck,
                     System.currentTimeMillis() - start);
    }

    private void publishFeatureNotificationRequestEvents(Collection<FeatureUniformResourceName> featureUrns,
                                                         Set<String> recipients) {
        publisher.publish(featureUrns.stream()
                                     .map(urn -> FeatureNotificationRequestEvent.build(owner,
                                                                                       urn,
                                                                                       PriorityLevel.HIGH,
                                                                                       recipients))
                                     .toList());
    }

}
