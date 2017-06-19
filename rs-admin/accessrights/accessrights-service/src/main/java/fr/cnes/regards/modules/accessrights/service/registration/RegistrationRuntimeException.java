/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.registration;

/**
 * Specific runtime exception
 * @author Xavier-Alexandre Brochard
 */
public class RegistrationRuntimeException extends RuntimeException {

    /**
     * Constructor
     */
    public RegistrationRuntimeException() {
        super("An exception occured during registration process");
    }

}
