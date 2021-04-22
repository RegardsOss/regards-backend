package fr.cnes.regards.modules.storage.client;

import java.util.Collection;
import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEventType;
import fr.cnes.regards.modules.storage.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storage.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storage.domain.flow.StorageFlowItem;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class FileReferenceEventDTO {

    /**
     * Business request identifier associated to the {@link FileReference}. Those identifiers are the identifier of file request.
     * See {@link StorageFlowItem}, {@link DeletionFlowItem} and {@link ReferenceFlowItem} for more information about
     * file requests.
     */
    private final Set<String> groupIds;

    /**
     * Checksum of the {@link FileReference}
     */
    private final String checksum;

    /**
     * Event type
     */
    private final FileReferenceEventType type;

    /**
     * Event message
     */
    private final String message;

    /**
     * Owners of the {@link FileReference}
     */
    private final Collection<String> owners;

    /**
     * location of the {@link FileReference}
     */
    private final FileLocation location;

    /**
     * Meta information of the {@link FileReference}
     */
    private final FileReferenceMetaInfo metaInfo;

    /**
     * Origin storage of the file requested
     */
    private final String originStorage;

    public FileReferenceEventDTO(FileReferenceEvent event) {
        this.checksum = event.getChecksum();
        this.groupIds = event.getGroupIds();
        this.location = event.getLocation();
        this.message = event.getMessage();
        this.metaInfo = event.getMetaInfo();
        this.originStorage = event.getOriginStorage();
        this.owners = event.getOwners();
        this.type = event.getType();
    }

    public String getChecksum() {
        return checksum;
    }

    public FileReferenceEventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public FileLocation getLocation() {
        return location;
    }

    public Collection<String> getOwners() {
        return owners;
    }

    public Set<String> getGroupIds() {
        return groupIds;
    }

    public FileReferenceMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public String getOriginStorage() {
        return originStorage;
    }
}
