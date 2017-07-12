/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notification.fallback;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;

/**
 * Hystrix fallback for Feign {@link INotificationClient}. This default implementation is executed when the circuit is
 * open or there is an error.<br>
 * To enable this fallback, set the fallback attribute to this class name in {@link INotificationClient}.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class NotificationFallback implements INotificationClient {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(NotificationFallback.class);

    /**
     * Common error message to log
     */
    private static final String FALLBACK_ERROR_MESSAGE = "RS-ADMIN / Notification request error. Fallback.";

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.signature.INotificationSignature#retrieveNotifications()
     */
    @Override
    public ResponseEntity<List<Notification>> retrieveNotifications() {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.notification.signature.INotificationSignature#createNotification(fr.cnes.regards.modules.
     * notification.domain.dto.NotificationDTO)
     */
    @Override
    public ResponseEntity<Notification> createNotification(final NotificationDTO pDto) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.signature.INotificationSignature#retrieveNotification(java.lang.Long)
     */
    @Override
    public ResponseEntity<Notification> retrieveNotification(final Long pId) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.notification.signature.INotificationSignature#updateNotificationStatus(java.lang.Long,
     * fr.cnes.regards.modules.notification.domain.NotificationStatus)
     */
    @Override
    public void updateNotificationStatus(final Long pId, final NotificationStatus pStatus) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.signature.INotificationSignature#deleteNotification(java.lang.Long)
     */
    @Override
    public void deleteNotification(final Long pId) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.signature.INotificationSignature#retrieveNotificationSettings()
     */
    @Override
    public ResponseEntity<NotificationSettings> retrieveNotificationSettings() {
        LOG.error(FALLBACK_ERROR_MESSAGE);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.notification.signature.INotificationSignature#updateNotificationSettings(fr.cnes.regards.
     * modules.notification.domain.dto.NotificationSettingsDTO)
     */
    @Override
    public void updateNotificationSettings(final NotificationSettingsDTO pDto) {
        LOG.error(FALLBACK_ERROR_MESSAGE);
    }

}
