package fr.cnes.regards.modules.ingest.dto.request;

import fr.cnes.regards.modules.ingest.domain.sip.VersioningMode;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class ChooseVersioningRequestParameters extends SearchRequestsParameters {

    private VersioningMode newVersioningMode;

    public static ChooseVersioningRequestParameters build() {
        return new ChooseVersioningRequestParameters();
    }

    public VersioningMode getNewVersioningMode() {
        return newVersioningMode;
    }

    public void setNewVersioningMode(VersioningMode newVersioningMode) {
        this.newVersioningMode = newVersioningMode;
    }

    public ChooseVersioningRequestParameters withNewVersioningMode(VersioningMode versioningMode) {
        this.setNewVersioningMode(versioningMode);
        return this;
    }
}
