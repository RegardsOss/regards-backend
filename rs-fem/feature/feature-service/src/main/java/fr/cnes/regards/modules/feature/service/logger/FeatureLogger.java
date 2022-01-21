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
package fr.cnes.regards.modules.feature.service.logger;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;

/**
 *
 * Centralize log feature life cycle events
 * @author Marc SORDI
 *
 */
public class FeatureLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureLogger.class);

    private static final String PREFIX = "[MONITORING] ";

    private static final String PARAM_PREFIX = "";

    private static final String PARAM = " | %s";

    private static final String PX2 = PARAM_PREFIX + PARAM + PARAM;

    private static final String PX3 = PX2 + PARAM;

    private static final String PX4 = PX3 + PARAM;

    private static final String PX5 = PX4 + PARAM;

    private static final String CREATION_DENIED_FORMAT = PREFIX + "Feature CREATION DENIED" + PX4;

    private static final String CREATION_GRANTED_FORMAT = PREFIX + "Feature CREATION GRANTED" + PX3;

    private static final String CREATION_SUCCESS_FORMAT = PREFIX + "Feature CREATED" + PX4;

    private static final String UPDATE_DENIED_FORMAT = PREFIX + "Feature UPDATE DENIED" + PX5;

    private static final String UPDATE_GRANTED_FORMAT = PREFIX + "Feature UPDATE GRANTED" + PX4;

    private static final String UPDATE_SUCCESS_FORMAT = PREFIX + "Feature UPDATED" + PX4;

    private static final String UPDATE_ERROR_FORMAT = PREFIX + "Feature UPDATE ERROR" + PX5;

    private static final String DELETION_DENIED_FORMAT = PREFIX + "Feature DELETION DENIED" + PX4;

    private static final String DELETION_GRANTED_FORMAT = PREFIX + "Feature DELETION GRANTED" + PX3;

    private static final String DELETION_SUCCESS_FORMAT = PREFIX + "Feature DELETED" + PX3;

    private static final String NOTIFICATION_DENIED_FORMAT = PREFIX + "Feature NOTIFICATION DENIED" + PX4;

    private static final String NOTIFICATION_GRANTED_FORMAT = PREFIX + "Feature NOTIFICATION GRANTED" + PX3;

    private static final String NOTIFICATION_SUCCESS_FORMAT = PREFIX + "Feature NOTIFIED" + PX3;

    private static final String NOTIFICATION_ERROR_FORMAT = PREFIX + "Feature Notification ERROR" + PX3;

    private FeatureLogger() {}

    public static void creationDenied(String requestOwner, String requestId, String providerId, Set<String> errors) {
        LOGGER.error(String.format(CREATION_DENIED_FORMAT, requestOwner, requestId, providerId, errors));
    }

    public static void creationGranted(String requestOwner, String requestId, String providerId) {
        LOGGER.trace(String.format(CREATION_GRANTED_FORMAT, requestOwner, requestId, providerId));
    }

    public static void creationSuccess(String requestOwner, String requestId, String providerId,
            FeatureUniformResourceName urn) {
        LOGGER.info(String.format(CREATION_SUCCESS_FORMAT, requestOwner, requestId, providerId, urn));
    }

    public static void updateDenied(String requestOwner, String requestId, String providerId,
            FeatureUniformResourceName urn, Set<String> errors) {
        LOGGER.error(String.format(UPDATE_DENIED_FORMAT, requestOwner, requestId, providerId, urn, errors));
    }

    public static void updateGranted(String requestOwner, String requestId, String providerId,
            FeatureUniformResourceName urn) {
        LOGGER.trace(String.format(UPDATE_GRANTED_FORMAT, requestOwner, requestId, providerId, urn));
    }

    public static void updateSuccess(String requestOwner, String requestId, String providerId,
            FeatureUniformResourceName urn) {
        LOGGER.info(String.format(UPDATE_SUCCESS_FORMAT, requestOwner, requestId, providerId, urn));
    }

    public static void updateError(String requestOwner, String requestId, String providerId,
            FeatureUniformResourceName urn, Set<String> errors) {
        LOGGER.error(String.format(UPDATE_ERROR_FORMAT, requestOwner, requestId, providerId, urn, errors));
    }

    public static void deletionDenied(String requestOwner, String requestId, FeatureUniformResourceName urn,
            Set<String> errors) {
        LOGGER.error(String.format(DELETION_DENIED_FORMAT, requestOwner, requestId, urn, errors));
    }

    public static void deletionGranted(String requestOwner, String requestId, FeatureUniformResourceName urn) {
        LOGGER.trace(String.format(DELETION_GRANTED_FORMAT, requestOwner, requestId, urn));
    }

    public static void deletionSuccess(String requestOwner, String requestId, FeatureUniformResourceName urn) {
        LOGGER.info(String.format(DELETION_SUCCESS_FORMAT, requestOwner, requestId, urn));
    }

    public static void notificationDenied(String requestOwner, String requestId, FeatureUniformResourceName urn,
            Set<String> errors) {
        LOGGER.error(String.format(NOTIFICATION_DENIED_FORMAT, requestOwner, requestId, urn, errors));
    }

    public static void notificationGranted(String requestOwner, String requestId, FeatureUniformResourceName urn) {
        LOGGER.trace(String.format(NOTIFICATION_GRANTED_FORMAT, requestOwner, requestId, urn));
    }

    public static void notificationSuccess(String requestOwner, String requestId, FeatureUniformResourceName urn) {
        LOGGER.trace(String.format(NOTIFICATION_SUCCESS_FORMAT, requestOwner, requestId, urn));
    }

    public static void notificationError(String requestOwner, String requestId, FeatureUniformResourceName urn) {
        LOGGER.error(String.format(NOTIFICATION_ERROR_FORMAT, requestOwner, requestId, urn));
    }
}
