/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 * Thrown if a joined search has too many results
 *
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class TooManyResultsException extends SearchException {

    public TooManyResultsException(String pErrorMessage) {
        super(pErrorMessage);
    }
}
