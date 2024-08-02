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
package fr.cnes.regards.modules.feature.service.request;

import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.service.IFeatureNotificationService;
import fr.cnes.regards.modules.feature.service.IFeatureUpdateService;
import fr.cnes.regards.modules.notifier.client.INotifierRequestListener;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class that handle {@link NotifierEvent}, which contains infos about Notifier requests it handled
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class FeatureNotifierListener implements INotifierRequestListener {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureNotifierListener.class);

    public static final String RECEIVED_FROM_NOTIFIER_FORMAT = "Received {} {} indicating {} to handle from rs-notifier";

    public static final String HANDLED_FROM_NOTIFIER_FORMAT = "Handled {} {} {}";

    @Autowired
    private IFeatureNotificationService featureNotificationService;

    @Autowired
    private IFeatureUpdateService featureUpdateService;

    @Autowired
    private FeatureUpdateDisseminationService featureUpdateDisseminationService;

    @Autowired
    private IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepo;

    @Override
    public void onRequestDenied(List<NotifierEvent> denied) {
        handleNotificationIssue(denied);
    }

    private void handleNotificationIssue(List<NotifierEvent> denied) {
        LOG.debug(RECEIVED_FROM_NOTIFIER_FORMAT,
                  denied.size(),
                  NotifierEvent.class.getSimpleName(),
                  NotificationState.ERROR);
        List<String> requestIds = denied.stream().map(NotifierEvent::getRequestId).collect(Collectors.toList());
        Set<AbstractFeatureRequest> errorRequest = abstractFeatureRequestRepo.findAllByRequestIdIn(requestIds);
        if (!errorRequest.isEmpty()) {
            featureNotificationService.handleNotificationError(errorRequest,
                                                               FeatureRequestStep.REMOTE_NOTIFICATION_ERROR);
            LOG.debug(HANDLED_FROM_NOTIFIER_FORMAT,
                      denied.size(),
                      NotificationState.ERROR,
                      NotifierEvent.class.getSimpleName());
        }
    }

    @Override
    public void onRequestGranted(List<NotifierEvent> granted) {
        // Do nothing
    }

    @Override
    public void onRequestSuccess(List<NotifierEvent> successNotifierEvts) {
        LOG.debug(RECEIVED_FROM_NOTIFIER_FORMAT,
                  successNotifierEvts.size(),
                  NotifierEvent.class.getSimpleName(),
                  NotificationState.SUCCESS);

        List<String> requestIds = successNotifierEvts.stream()
                                                     .map(NotifierEvent::getRequestId)
                                                     .collect(Collectors.toList());
        Set<AbstractFeatureRequest> associatedFeatureRequests = abstractFeatureRequestRepo.findAllByRequestIdIn(
            requestIds);
        featureUpdateDisseminationService.savePutRequests(successNotifierEvts, associatedFeatureRequests);
        handleFeatureNotificationRequests(successNotifierEvts, associatedFeatureRequests);
    }

    private void handleFeatureNotificationRequests(List<NotifierEvent> successNotifierEvts,
                                                   Set<AbstractFeatureRequest> associatedFeatureRequests) {
        if (!associatedFeatureRequests.isEmpty()) {
            featureNotificationService.handleNotificationSuccess(associatedFeatureRequests);
            LOG.debug(HANDLED_FROM_NOTIFIER_FORMAT,
                      successNotifierEvts.size(),
                      NotificationState.SUCCESS,
                      NotifierEvent.class.getSimpleName());
        }
    }

    @Override
    public void onRequestError(List<NotifierEvent> error) {
        handleNotificationIssue(error);
    }
}
