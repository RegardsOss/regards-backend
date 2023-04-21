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
package fr.cnes.regards.modules.notifier.service.job;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.dto.out.Recipient;
import fr.cnes.regards.modules.notifier.service.NotificationProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Job implementation to notify {@link Recipient} according {@link Rule}
 *
 * @author Kevin Marchois
 */
public class NotificationJob extends AbstractJob<Void> {

    public static final String NOTIFICATION_REQUEST_IDS = "ids";

    public static final String RECIPIENT_BUSINESS_ID = "recipient_id";

    private List<NotificationRequest> notificationRequests;

    private PluginConfiguration recipient;

    @Autowired
    private INotificationRequestRepository notificationRequestRepo;

    @Autowired
    private NotificationProcessingService notificationProcessingService;

    @Autowired
    private IPluginService pluginService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {

        Type type = new TypeToken<Set<Long>>() {

        }.getType();
        notificationRequests = notificationRequestRepo.findAllById(getValue(parameters,
                                                                            NOTIFICATION_REQUEST_IDS,
                                                                            type));
        recipient = pluginService.loadPluginConfiguration(getValue(parameters, RECIPIENT_BUSINESS_ID, String.class));
    }

    @Override
    public void run() {
        logger.info("[{}] Notification job starts", jobInfoId);
        long start = System.currentTimeMillis();
        
        Pair<Integer, Integer> processResult = notificationProcessingService.processRequests(notificationRequests,
                                                                                             recipient);
        logger.info("[{}]{}{} Notifications sent to <{} - {} - {}> in {} ms, {} notifications failed",
                    jobInfoId,
                    INFO_TAB,
                    processResult.getFirst(),
                    recipient.getBusinessId(),
                    recipient.getPluginId(),
                    recipient.getLabel(),
                    System.currentTimeMillis() - start,
                    processResult.getSecond());
        // if there are exception we throw an exception to stop the job in error
        if (!processResult.getSecond().equals(0)) {
            throw new RsRuntimeException(String.format(
                "Some notifications (%s) failed for the Job %s for recipient <%s - %s - %s>",
                processResult.getSecond(),
                jobInfoId,
                recipient.getBusinessId(),
                recipient.getPluginId(),
                recipient.getLabel()));
        }
    }

}
