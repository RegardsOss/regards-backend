/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.rest;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.utils.endpoint.annotation.ResourceAccess;
import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.rest.Controller;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;
import fr.cnes.regards.modules.notification.domain.dto.NotificationSettingsDTO;
import fr.cnes.regards.modules.notification.service.INotificationService;
import fr.cnes.regards.modules.notification.service.INotificationSettingsService;
import fr.cnes.regards.modules.notification.signature.INotificationSignature;

/**
 * Controller defining the REST entry points of the module
 *
 * @author CS SI
 *
 */
@RestController
@ModuleInfo(name = "notification", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class NotificationController extends Controller implements INotificationSignature {

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

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.signature.INotificationSignature#retrieveNotifications()
     */
    @Override
    @ResourceAccess(description = "Retrieve the list of notifications for the logged user", name = "notification")
    public ResponseEntity<List<Notification>> retrieveNotifications() {
        final List<Notification> notifications = notificationService.retrieveNotifications();
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.notification.signature.INotificationSignature#sendNotification(fr.cnes.regards.modules.
     * notification.domain.NotificationDTO)
     */
    @Override
    @ResourceAccess(description = "Define the endpoint for sending an notification to recipients",
            name = "notification")
    public ResponseEntity<Notification> createNotification(@Valid @RequestBody final NotificationDTO pDto) {
        final Notification notification = notificationService.createNotification(pDto);
        return new ResponseEntity<>(notification, HttpStatus.CREATED);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.signature.INotificationSignature#retrieveNotification(java.lang.Long)
     */
    @Override
    @ResourceAccess(description = "Define the endpoint for retrieving a notification", name = "notification")
    public ResponseEntity<Notification> retrieveNotification(@PathVariable("notification_id") final Long pId)
            throws EntityNotFoundException {
        final Notification notification = notificationService.retrieveNotification(pId);
        return new ResponseEntity<>(notification, HttpStatus.OK);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.notification.signature.INotificationSignature#updateNotificationStatus(java.lang.Long)
     */
    @Override
    @ResourceAccess(description = "Define the endpoint for updating the notification status", name = "notification")
    public void updateNotificationStatus(@PathVariable("notification_id") final Long pId,
            @Valid @RequestBody final NotificationStatus pStatus) throws EntityNotFoundException {
        notificationService.updateNotificationStatus(pId, pStatus);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.signature.INotificationSignature#deleteNotification(java.lang.Long)
     */
    @Override
    @ResourceAccess(description = "Define the endpoint for deleting a notification", name = "notification")
    public void deleteNotification(@PathVariable("notification_id") final Long pId) throws EntityNotFoundException {
        notificationService.deleteNotification(pId);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.notification.signature.INotificationSignature#retrieveNotificationSettings()
     */
    @Override
    @ResourceAccess(description = "Define the endpoint for retrieving the notification settings for the logged user",
            name = "notification")
    public ResponseEntity<NotificationSettings> retrieveNotificationSettings() {
        final NotificationSettings settings = notificationSettingsService.retrieveNotificationSettings();
        return new ResponseEntity<>(settings, HttpStatus.OK);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.notification.signature.INotificationSignature#updateNotificationSettings(fr.cnes.regards.
     * modules.notification.domain.NotificationSettings)
     */
    @Override
    @ResourceAccess(description = "Define the endpoint for updating the notification status", name = "notification")
    public void updateNotificationSettings(final NotificationSettingsDTO pNotificationSettings) {
        notificationSettingsService.updateNotificationSettings(pNotificationSettings);
    }

}