/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.maintenance;

/**
 * If this exception is thrown by whatever module, the maintenance mode is activated in current tenant.
 *
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class MaintenanceException extends RuntimeException {

    public MaintenanceException(final String pMessage, final Throwable pCause) {
        super(pMessage, pCause);
    }

    public MaintenanceException(final String pMessage) {
        super(pMessage);
    }
}
