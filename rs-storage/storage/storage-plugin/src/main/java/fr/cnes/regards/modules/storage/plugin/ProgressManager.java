package fr.cnes.regards.modules.storage.plugin;

import java.net.URL;

import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

public class ProgressManager {

    boolean errorStatus = false;

    public void storageSucceed(AIP aip, DataFile datafile, URL storedUrl) {
        // TODO : Send AMQP Event
    }

    public void storageFailed(AIP aip, DataFile datafile) {
        // TODO : Send AMQP Event
        errorStatus = true;
    }

    public Boolean isProcessError() {
        return errorStatus;
    }

}
