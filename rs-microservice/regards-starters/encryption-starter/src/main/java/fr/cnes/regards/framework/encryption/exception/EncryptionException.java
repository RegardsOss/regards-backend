package fr.cnes.regards.framework.encryption.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Wrapper for exceptions that could occur during encryption/decryption process
 * @author Sylvain VISSIERE-GUERINET
 */
public class EncryptionException extends ModuleException {

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
