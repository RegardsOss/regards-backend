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
package fr.cnes.regards.modules.notification.rest;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;
import fr.cnes.regards.modules.notification.service.INotificationService;
import fr.cnes.regards.modules.notification.service.INotificationSettingsService;

/**
 * Controller defining the REST entry points of the module
 *
 * @author Xavier-Alexandre Brochard
 *
 */
@RestController
@ModuleInfo(name = "notification", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("/notifications")
public class NotificationController {

    /**
     * The service responsible for managing notifications
     */
    @Autowired
    private INotificationService notificationService;

    /**
     * The service responsible for handling CRUD operations on notification settings
     */
    @Autowired
    private INotificationSettingsService notificationSettingsService;

    /**
     * Define the endpoint for retrieving the list of notifications for the logged user
     *
     * @return A {@link List} of {@link Notification} wrapped in a {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             thrown when no current user could be found
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve the list of notifications for the logged user")
    public ResponseEntity<List<Notification>> retrieveNotifications() throws EntityNotFoundException {
        final List<Notification> notifications = notificationService.retrieveNotifications();
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }

    /**
     * Define the endpoint for creating a new notification in db for later sending by a scheluder.
     *
     * @param pDto
     *            A DTO for easy parsing of the response body. Mapping to true {@link Notification} is done in service.
     * @return The sent notification as {@link Notification} wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Define the endpoint for sending an notification to recipients")
    public ResponseEntity<Notification> createNotification(@Valid @RequestBody final NotificationDTO pDto) {
        final Notification notification = notificationService.createNotification(pDto);
        return new ResponseEntity<>(notification, HttpStatus.CREATED);
    }

    /**
     * Define the endpoint for retrieving a notification
     *
     * @param pId
     *            The notification <code>id</code>
     * @throws EntityNotFoundException
     *             Thrown when no notification with passed <code>id</code> could be found
     * @return The {@link Notification} wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(value = "/{notification_id}", method = RequestMethod.GET)
    @ResourceAccess(description = "Define the endpoint for retrieving a notification")
    public ResponseEntity<Notification> retrieveNotification(@PathVariable("notification_id") final Long pId)
            throws EntityNotFoundException {
        final Notification notification = notificationService.retrieveNotification(pId);
        return new ResponseEntity<>(notification, HttpStatus.OK);
    }

    /**
     * Define the endpoint for updating the {@link Notification#status}
     *
     * @param pId
     *            The notification <code>id</code>
     * @param pStatus
     *            The new <code>status</code>
     * @return The updated {@link Notification} wrapped in a {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             Thrown when no notification with passed <code>id</code> could be found
     *
     */
    @ResponseBody
    @RequestMapping(value = "/{notification_id}", method = RequestMethod.PUT)
    @ResourceAccess(description = "Define the endpoint for updating the notification status")
    public ResponseEntity<Notification> updateNotificationStatus(@PathVariable("notification_id") final Long pId,
            @Valid @RequestBody final NotificationStatus pStatus) throws EntityNotFoundException {
        final Notification notification = notificationService.updateNotificationStatus(pId, pStatus);
        return new ResponseEntity<>(notification, HttpStatus.OK);
    }

    /**
     * Define the endpoint for deleting a notification
     *
     * @param pId
     *            The notification <code>id</code>
     * @throws EntityNotFoundException
     *             Thrown when no notification with passed <code>id</code> could be found
     * @return void
     */
    @RequestMapping(value = "/{notification_id}", method = RequestMethod.DELETE)
    @ResourceAccess(description = "Define the endpoint for deleting a notification")
    public ResponseEntity<Void> deleteNotification(@PathVariable("notification_id") final Long pId)
            throws EntityNotFoundException {
        notificationService.deleteNotification(pId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Define the endpoint for retrieving the notification configuration parameters for the logged user
     *
     * @return The {@link NotificationSettings} wrapped in a {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             thrown when no current user could be found
     */
    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    @ResourceAccess(description = "Define the endpoint for retrieving the notification settings for the logged user")
    public ResponseEntity<NotificationSettings> retrieveNotificationSettings() throws EntityNotFoundException {
        final NotificationSettings settings = notificationSettingsService.retrieveNotificationSettings();
        return new ResponseEntity<>(settings, HttpStatus.OK);
    }

    /**
     * Define the endpoint for updating the {@link Notification#status}
     *
     * @param pNotificationSettings
     *            The facade exposing user updatable fields of notification settings
     * @return The updated {@link NotificationSettings} wrapped in a {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             Thrown when no notification settings with passed <code>id</code> could be found
     */
    @RequestMapping(value = "/settings", method = RequestMethod.PUT)
    @ResourceAccess(description = "Define the endpoint for updating the notification status")
    public ResponseEntity<NotificationSettings> updateNotificationSettings(
            final NotificationSettingsDTO pNotificationSettings) throws EntityNotFoundException {
        final NotificationSettings settings = notificationSettingsService
                .updateNotificationSettings(pNotificationSettings);
        return new ResponseEntity<>(settings, HttpStatus.OK);
    }

}