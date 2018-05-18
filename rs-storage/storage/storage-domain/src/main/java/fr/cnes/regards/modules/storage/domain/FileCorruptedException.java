package fr.cnes.regards.modules.storage.domain;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Exception used when a file was corrupted while it is handled
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class FileCorruptedException extends ModuleException {

    /**
     * Inherited constructor from {@link ModuleException}
     * @param pErrorMessage
     */
    public FileCorruptedException(String pErrorMessage) {
        super(pErrorMessage);
    }

    /**
     * Inherited constructor from {@link ModuleException}
     * @param pCause
     */
    public FileCorruptedException(Throwable pCause) {
        super(pCause);
    }

    /**
     * Inherited constructor from {@link ModuleException}
     * @param pMessage
     * @param pCause
     */
    public FileCorruptedException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
