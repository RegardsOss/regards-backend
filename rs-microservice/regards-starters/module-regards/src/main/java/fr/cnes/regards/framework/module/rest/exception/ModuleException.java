/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 * Main module exception
 *
 * @author Marc Sordi
 *
 */
public class ModuleException extends Exception {

    private static final long serialVersionUID = 100L;

    public ModuleException(String pErrorMessage) {
        super(pErrorMessage);
    }

    public ModuleException(Throwable pCause) {
        super(pCause);
    }

    public ModuleException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }

}
