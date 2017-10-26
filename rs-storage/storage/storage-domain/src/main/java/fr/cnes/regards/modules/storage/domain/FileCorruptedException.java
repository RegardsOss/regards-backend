package fr.cnes.regards.modules.storage.domain;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class FileCorruptedException extends ModuleException {

    public FileCorruptedException(String pErrorMessage) {
        super(pErrorMessage);
    }

    public FileCorruptedException(Throwable pCause) {
        super(pCause);
    }

    public FileCorruptedException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
