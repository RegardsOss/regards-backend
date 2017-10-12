package fr.cnes.regards.modules.storage.plugin.datastorage.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class StorageCorruptedException extends ModuleException{

    public StorageCorruptedException(String pErrorMessage) {
        super(pErrorMessage);
    }

    public StorageCorruptedException(Throwable pCause) {
        super(pCause);
    }

    public StorageCorruptedException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
