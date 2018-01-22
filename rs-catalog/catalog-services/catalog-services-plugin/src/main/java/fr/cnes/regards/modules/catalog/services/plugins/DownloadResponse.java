package fr.cnes.regards.modules.catalog.services.plugins;

import java.util.List;

public class DownloadResponse {

    private final List<String> toDownload;

    private final List<String> other;

    public DownloadResponse(List<String> toDownload, List<String> other) {
        super();
        this.toDownload = toDownload;
        this.other = other;
    }

}
