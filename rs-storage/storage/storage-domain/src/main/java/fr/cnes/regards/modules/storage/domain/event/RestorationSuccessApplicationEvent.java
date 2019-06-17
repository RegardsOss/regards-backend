package fr.cnes.regards.modules.storage.domain.event;

import java.nio.file.Path;

import org.springframework.context.ApplicationEvent;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * Spring event, that only propagates through current microservice instance, allowing to do some action after a successful restoration.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class RestorationSuccessApplicationEvent extends ApplicationEvent {

    private final Path restorationPath;

    private final Long storageConfId;

    private final String tenant;

    public RestorationSuccessApplicationEvent(StorageDataFile data, Path restorationPath, Long storageConfId,
            String tenant) {
        super(data);
        this.restorationPath = restorationPath;
        this.storageConfId = storageConfId;
        this.tenant = tenant;
    }

    public Path getRestorationPath() {
        return restorationPath;
    }

    public Long getStorageConfId() {
        return storageConfId;
    }

    public String getTenant() {
        return tenant;
    }
}
