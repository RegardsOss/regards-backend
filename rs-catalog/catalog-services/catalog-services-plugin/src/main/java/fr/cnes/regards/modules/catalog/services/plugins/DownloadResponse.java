package fr.cnes.regards.modules.catalog.services.plugins;

import java.util.List;

public class DownloadResponse {

    private final List<String> offlineFiles;

    public DownloadResponse(List<String> offlineFiles) {
        super();
        this.offlineFiles = offlineFiles;
    }

    public List<String> getOfflineFiles() {
        return offlineFiles;
    }

}
