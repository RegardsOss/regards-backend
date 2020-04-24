/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

import com.google.gson.reflect.TypeToken;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.notifier.dao.INotificationActionRepository;
import fr.cnes.regards.modules.notifier.domain.NotificationAction;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.service.INotificationRuleService;

/**
 * Job implementation to notify {@link Recipient} according {@link Rule}
 * @author Kevin Marchois
 *
 */
public class NotificationJob extends AbstractJob<Void> {

    public static final String IDS_PARAMETER = "ids";

    private List<NotificationAction> notificationRequests;

    @Autowired
    private INotificationActionRepository notificationActionRepo;

    @Autowired
    private INotificationRuleService notificationService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        Type type = new TypeToken<Set<Long>>() {

        }.getType();
        notificationRequests = this.notificationActionRepo.findAllById(getValue(parameters, IDS_PARAMETER, type));
    }

    @Override
    public void run() {
        LOGGER.info("[{}] Notification job starts", jobInfoId);
        long start = System.currentTimeMillis();
        Pair<Integer, Integer> notifications = this.notificationService.processRequest(notificationRequests, jobInfoId);
        LOGGER.info("[{}]{}{} Notifications sended in {} ms, {} notifications failed", jobInfoId, INFO_TAB,
                    notifications.getFirst(), System.currentTimeMillis() - start, notifications.getSecond());
        // if there are exception we throw an exception to stop the job in error
        if (!notifications.getSecond().equals(0)) {
            throw new RsRuntimeException(String.format("Some Recipient failed for the Job %s", jobInfoId));
        }
    }

}
