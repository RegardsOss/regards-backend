/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 *
 * Throws when connection to a datasource cannot be established
 *
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class InvalidConnectionException extends ModuleException {

    public InvalidConnectionException(Throwable pCause) {
        super(pCause);
    }
}
