package fr.cnes.regards.modules.storage.plugin;

import java.net.URL;
import java.util.Set;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

public class ProgressManager {

    private Set<String> failureCauses= Sets.newHashSet();

    private boolean errorStatus = false;

    public void storageSucceed(DataFile datafile, URL storedUrl) {
        // TODO : Send AMQP Event
    }

    public void storageFailed(DataFile datafile, String cause) {
        // TODO : Send AMQP Event
        failureCauses.add(cause);
        errorStatus = true;
    }

    public Boolean isProcessError() {
        return errorStatus;
    }

    public void deletionFailed(DataFile data, String failureCause) {

    }

    public void deletionSucceed(DataFile data) {

    }

    public Set<String> getFailureCauses() {
        return failureCauses;
    }
}
