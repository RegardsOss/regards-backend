package fr.cnes.regards.modules.storage.client;

import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceUpdateEvent;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class FileReferenceUpdateDTO {

    /**
     * Represent the old checksum of the reference updated in storage.
     * So this should be the current one for consumers
     */
    private final String checksum;

    /**
     * Represent the old storage of the reference updated in storage.
     * So this should be the current one for consumers
     */
    private final String storage;

    /**
     * Represent the updated reference in storage.
     * So information given here are the new truth
     */
    private final FileReference updatedRef;

    public FileReferenceUpdateDTO(FileReferenceUpdateEvent event) {
        this.checksum = event.getChecksum();
        this.storage = event.getStorage();
        this.updatedRef = event.getUpdatedFile();
    }

    public String getChecksum() {
        return checksum;
    }

    public String getStorage() {
        return storage;
    }

    public FileReference getUpdatedRef() {
        return updatedRef;
    }
}
