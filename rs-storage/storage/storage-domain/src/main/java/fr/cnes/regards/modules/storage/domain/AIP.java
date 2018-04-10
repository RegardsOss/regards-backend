/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.oais.AbstractInformationPackage;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;

/**
 *
 * Archival Information Package representation
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
public class AIP extends AbstractInformationPackage<UniformResourceName> {

    /**
     * SIP ID
     */
    private String sipId;

    /**
     * State determined through different storage steps
     */
    @GsonIgnore
    private AIPState state;

    @GsonIgnore
    private boolean retry;

    /**
     * Default constructor
     */
    public AIP() {
        super();
    }

    /**
     * @return the aip state
     */
    public AIPState getState() {
        return state;
    }

    /**
     * Set the aip state
     * @param state
     */
    public void setState(AIPState state) {
        this.state = state;
    }

    /**
     * @return the sip id
     */
    public String getSipId() {
        return sipId;
    }

    /**
     * Set the sip id
     * @param sipId
     */
    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }
}
