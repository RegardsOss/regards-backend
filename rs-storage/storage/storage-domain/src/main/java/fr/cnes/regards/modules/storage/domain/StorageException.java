package fr.cnes.regards.modules.storage.domain;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@SuppressWarnings("serial")
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }
}
