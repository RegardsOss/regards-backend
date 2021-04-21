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


package fr.cnes.regards.modules.ingest.service.notification;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;

/**
 * Centralize log for aip notification life cycle events
 * @author Iliana Ghazali
 */

public class AIPNotificationLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPNotificationLogger.class);

    private static final String PREFIX = "[MONITORING] ";

    private static final String PARAM_PREFIX = "";

    private static final String PARAM = " | %s";

    private static final String PX2 = PARAM_PREFIX + PARAM + PARAM;

    private static final String PX3 = PX2 + PARAM;

    private static final String NOTIFICATION_DENIED_FORMAT = PREFIX + "NOTIFICATION DENIED" + PX3;

    private static final String NOTIFICATION_ERROR_FORMAT = PREFIX + "NOTIFICATION ERROR" + PX3;

    private static final String NOTIFICATION_GRANTED_FORMAT = PREFIX + "NOTIFICATION GRANTED" + PX2;

    private static final String NOTIFICATION_SUCCESS_FORMAT = PREFIX + "NOTIFIED SUCCESSFULLY" + PX2;

    private static final String RECEIVED_FROM_NOTIFIER_FORMAT = "Received {} {} indicating {} to handle from rs-notifier";

    private static final String HANDLED_FROM_NOTIFIER_FORMAT = "Handled {} {} {}";

    private AIPNotificationLogger() {}

    public static void notificationDenied(Long requestId, String providerId, Set<String> errors) {
        LOGGER.error(String.format(NOTIFICATION_DENIED_FORMAT, requestId, providerId, errors));
    }

    public static void notificationGranted(Long requestId, String providerId) {
        LOGGER.trace(String.format(NOTIFICATION_GRANTED_FORMAT, requestId, providerId));
    }

    public static void notificationSuccess(Long requestId, String providerId) {
        LOGGER.trace(String.format(NOTIFICATION_SUCCESS_FORMAT, requestId, providerId));
    }

    public static void notificationError(Long requestId, String providerId, Set<String> errors) {
        LOGGER.trace(String.format(NOTIFICATION_ERROR_FORMAT, requestId, providerId, errors));
    }

    public static void notificationEventSuccess(int sizeEvents) {
        LOGGER.debug(RECEIVED_FROM_NOTIFIER_FORMAT, sizeEvents, NotifierEvent.class.getSimpleName(),
                     NotificationState.SUCCESS);
    }

    public static void notificationEventSuccessHandled(int sizeEvents) {
        LOGGER.debug(HANDLED_FROM_NOTIFIER_FORMAT, sizeEvents, NotificationState.SUCCESS,
                     NotifierEvent.class.getSimpleName());
    }

    public static void notificationEventError(int sizeEvents) {
        LOGGER.debug(RECEIVED_FROM_NOTIFIER_FORMAT, sizeEvents, NotifierEvent.class.getSimpleName(),
                     NotificationState.ERROR);
    }

    public static void notificationEventErrorHandled(int sizeEvents) {
        LOGGER.debug(HANDLED_FROM_NOTIFIER_FORMAT, sizeEvents, NotificationState.ERROR,
                     NotifierEvent.class.getSimpleName());
    }
}
