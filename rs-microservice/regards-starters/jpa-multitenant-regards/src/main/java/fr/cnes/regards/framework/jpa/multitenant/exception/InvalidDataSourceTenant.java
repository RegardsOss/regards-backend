/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.exception;

/**
 *
 * Base JPA multitenant exception
 *
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class InvalidDataSourceTenant extends RuntimeException {

    public InvalidDataSourceTenant(String message) {
        super(message);
    }
}
