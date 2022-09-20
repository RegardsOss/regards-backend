package fr.cnes.regards.modules.notification.domain;

/**
 * Indicate which mode should be used for the notfication: INSTANCE or MULTITENANT.
 * The only difference is that in mode MULTITENANT, project users exists and so we listen to project user and role events
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public enum NotificationMode {
    INSTANCE,
    MULTITENANT
}
