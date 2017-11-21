package fr.cnes.regards.modules.notification.domain;

/**
 * Describe the notification criticity.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public enum NotificationType {

    /**
     * Associated notification reflects an information
     */
    INFO,
    /**
     * Associated notification reflects an error
     */
    ERROR,
    /**
     * Associated notification reflects a fatal error
     */
    FATAL

}
