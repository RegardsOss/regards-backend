/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.notification.NotificationDTO;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notification.domain.INotificationWithoutMessage;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;
import fr.cnes.regards.modules.notification.rest.dto.NotificationSummary;
import fr.cnes.regards.modules.notification.service.INotificationService;
import fr.cnes.regards.modules.notification.service.INotificationSettingsService;

/**
 * Controller defining the REST entry points of the module
 * @author Xavier-Alexandre Brochard
 * @author Marc SORDI
 */
@RestController
@RequestMapping(NotificationController.NOTIFICATION_PATH)
public class NotificationController implements IResourceController<Notification> {

    /**
     * Controller base path
     */
    public static final String NOTIFICATION_PATH = "/notifications";

    /**
     * Controller path using notification id as path variable
     */
    public static final String NOTIFICATION_ID_PATH = "/{notification_id}";

    /**
     * Controller path to delete read notifications
     */
    public static final String NOTIFICATION_DELETE_PATH = "/read/delete";

    /**
     * Controller path using notification id as path variable
     */
    public static final String NOTIFICATION_READ_PATH = NOTIFICATION_ID_PATH + "/read";

    /**
     * Endpoint to acknowledge all notifications
     */
    public static final String NOTIFICATION_READ_ALL_PATH = "/all/read";

    /**
     * Controller path using notification id as path variable
     */
    public static final String NOTIFICATION_UNREAD_PATH = NOTIFICATION_ID_PATH + "/unread";

    /**
     * Endpoint to revert acknowledgement on all notifications
     */
    public static final String NOTIFICATION_UNREAD_ALL_PATH = "/all/unread";

    /**
     * Controller path for notification settings
     */
    public static final String NOTIFICATION_SETTINGS = "/settings";

    /**
     * Controller path to retrieve notifications summary. Used for long pooling by frontend.
     */
    public static final String SUMMARY = "/summary";

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
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Define the endpoint for retrieving the list of notifications for the logged user
     * @return A {@link List} of {@link Notification} wrapped in a {@link ResponseEntity}
     * @throws EntityNotFoundException thrown when no current user could be found
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve the list of notifications for the logged user",
            role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<PagedModel<EntityModel<INotificationWithoutMessage>>> retrieveNotifications(
            @RequestParam(name = "state", required = false) NotificationStatus state,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable page,
            PagedResourcesAssembler<INotificationWithoutMessage> assembler) throws EntityNotFoundException {
        Page<INotificationWithoutMessage> notifications = notificationService.retrieveNotifications(page, state);
        return new ResponseEntity<>(notifWithoutMsgPagedResources(notifications, assembler), HttpStatus.OK);
    }

    private PagedModel<EntityModel<INotificationWithoutMessage>> notifWithoutMsgPagedResources(
            Page<INotificationWithoutMessage> notifications,
            PagedResourcesAssembler<INotificationWithoutMessage> assembler) {
        final PagedModel<EntityModel<INotificationWithoutMessage>> pageResources = assembler.toModel(notifications);
        pageResources.forEach(resource -> resource.add(notifWithoutMsgToResource(resource.getContent()).getLinks()));
        return pageResources;
    }

    /**
     * Define the endpoint for creating a new notification in db for later sending by a scheluder.
     * @param dto A DTO for easy parsing of the response body. Mapping to true {@link Notification} is done in service.
     * @return The sent notification as {@link Notification} wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Define the endpoint for sending an notification to recipients")
    public ResponseEntity<Notification> createNotification(@Valid @RequestBody NotificationDTO dto) {
        Notification notification = notificationService.createNotification(dto);
        return new ResponseEntity<>(notification, HttpStatus.CREATED);
    }

    /**
     * Define the endpoint for retrieving a notification
     * @param id The notification <code>id</code>
     * @return The {@link Notification} wrapped in a {@link ResponseEntity}
     * @throws EntityNotFoundException Thrown when no notification with passed <code>id</code> could be found
     */
    @RequestMapping(value = NOTIFICATION_ID_PATH, method = RequestMethod.GET)
    @ResourceAccess(description = "Define the endpoint for retrieving a notification",
            role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<Notification> retrieveNotification(@PathVariable("notification_id") Long id)
            throws EntityNotFoundException {
        Notification notification = notificationService.retrieveNotification(id);
        return new ResponseEntity<>(notification, HttpStatus.OK);
    }

    /**
     * Allows to set a notification to status read
     * @param id The notification <code>id</code>
     * @return The updated {@link Notification} wrapped in a {@link ResponseEntity}
     * @throws EntityNotFoundException Thrown when no notification with passed <code>id</code> could be found
     */
    @RequestMapping(value = NOTIFICATION_READ_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Define the endpoint for updating the notification status",
            role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<Notification> setNotificationRead(@PathVariable("notification_id") Long id)
            throws EntityNotFoundException {
        Notification notification = notificationService.updateNotificationStatus(id, NotificationStatus.READ);
        return new ResponseEntity<>(notification, HttpStatus.OK);
    }

    /**
     * Allows to set all notification to status read
     */
    @RequestMapping(value = NOTIFICATION_READ_ALL_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Set all unread notification as read", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<Void> markAllNotificationAsRead() {
        notificationService.markAllNotificationAs(NotificationStatus.READ);
        return ResponseEntity.noContent().build();
    }

    /**
     * Allows to set a notification to status unread
     * @param id The notification <code>id</code>
     * @return The updated {@link Notification} wrapped in a {@link ResponseEntity}
     * @throws EntityNotFoundException Thrown when no notification with passed <code>id</code> could be found
     */
    @RequestMapping(value = NOTIFICATION_UNREAD_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Define the endpoint for updating the notification status",
            role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<Notification> setNotificationUnRead(@PathVariable("notification_id") Long id)
            throws EntityNotFoundException {
        Notification notification = notificationService.updateNotificationStatus(id, NotificationStatus.UNREAD);
        return new ResponseEntity<>(notification, HttpStatus.OK);
    }

    /**
     * Allows to set all notification to status read
     */
    @RequestMapping(value = NOTIFICATION_UNREAD_ALL_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Set all unread notification as read", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<Void> markAllNotificationAsUnread() {
        notificationService.markAllNotificationAs(NotificationStatus.UNREAD);
        return ResponseEntity.noContent().build();
    }

    /**
     * Define the endpoint for deleting a notification
     * @param id The notification <code>id</code>
     * @return void
     * @throws EntityNotFoundException Thrown when no notification with passed <code>id</code> could be found
     */
    @RequestMapping(value = NOTIFICATION_ID_PATH, method = RequestMethod.DELETE)
    @ResourceAccess(description = "Define the endpoint for deleting a notification", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> deleteNotification(@PathVariable("notification_id") Long id)
            throws EntityNotFoundException {
        notificationService.deleteNotification(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Define the endpoint for deleting a notification
     * @return void
     * @throws EntityNotFoundException Thrown when no notification with passed <code>id</code> could be found
     */
    @RequestMapping(value = NOTIFICATION_DELETE_PATH, method = RequestMethod.DELETE)
    @ResourceAccess(description = "Define the endpoint for deleting a notification", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> deleteNotification() throws EntityNotFoundException {
        notificationService.deleteReadNotifications();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Define the endpoint for retrieving the notification configuration parameters for the logged user
     * @return The {@link NotificationSettings} wrapped in a {@link ResponseEntity}
     * @throws EntityNotFoundException thrown when no current user could be found
     */
    @RequestMapping(value = NOTIFICATION_SETTINGS, method = RequestMethod.GET)
    @ResourceAccess(description = "Define the endpoint for retrieving the notification settings for the logged user",
            role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<NotificationSettings> retrieveNotificationSettings() throws EntityNotFoundException {
        NotificationSettings settings = notificationSettingsService.retrieveNotificationSettings();
        return new ResponseEntity<>(settings, HttpStatus.OK);
    }

    /**
     * Define the endpoint for updating the {@link Notification#getStatus()}
     * @param notificationSettings The facade exposing user updatable fields of notification settings
     * @return The updated {@link NotificationSettings} wrapped in a {@link ResponseEntity}
     * @throws EntityNotFoundException Thrown when no notification settings with passed <code>id</code> could be found
     */
    @RequestMapping(value = NOTIFICATION_SETTINGS, method = RequestMethod.PUT)
    @ResourceAccess(description = "Define the endpoint for updating the notification settings",
            role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<NotificationSettings> updateNotificationSettings(
            @RequestBody NotificationSettingsDTO notificationSettings) throws EntityNotFoundException {
        NotificationSettings settings = notificationSettingsService.updateNotificationSettings(notificationSettings);
        return new ResponseEntity<>(settings, HttpStatus.OK);
    }

    @RequestMapping(value = SUMMARY, method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve summary infos about notifications", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<NotificationSummary> summary() throws EntityNotFoundException {
        Long unReads = notificationService.countUnreadNotifications();
        Long reads = notificationService.countReadNotifications();
        return new ResponseEntity<>(new NotificationSummary(unReads, reads), HttpStatus.OK);
    }

    @Override
    public EntityModel<Notification> toResource(Notification element, Object... extras) {
        EntityModel<Notification> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveNotification", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource, this.getClass(), "deleteNotification", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, element.getId()));
        if (element.getStatus().equals(NotificationStatus.UNREAD)) {
            resourceService.addLink(resource, this.getClass(), "setNotificationRead", LinkRelation.of("read"),
                                    MethodParamFactory.build(Long.class, element.getId()));
        } else {
            resourceService.addLink(resource, this.getClass(), "setNotificationUnRead", LinkRelation.of("unread"),
                                    MethodParamFactory.build(Long.class, element.getId()));
        }
        return resource;
    }

    public EntityModel<INotificationWithoutMessage> notifWithoutMsgToResource(INotificationWithoutMessage element,
            Object... extras) {
        EntityModel<INotificationWithoutMessage> resource = new EntityModel<>(element);
        resourceService.addLink(resource, this.getClass(), "retrieveNotification", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource, this.getClass(), "deleteNotification", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, element.getId()));
        if (element.getStatus().equals(NotificationStatus.UNREAD)) {
            resourceService.addLink(resource, this.getClass(), "setNotificationRead", LinkRelation.of("read"),
                                    MethodParamFactory.build(Long.class, element.getId()));
        } else {
            resourceService.addLink(resource, this.getClass(), "setNotificationUnRead", LinkRelation.of("unread"),
                                    MethodParamFactory.build(Long.class, element.getId()));
        }
        return resource;
    }

}