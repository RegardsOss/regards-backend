/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.storage.domain;

import java.util.Optional;

import org.hibernate.validator.constraints.NotBlank;

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
     * Provider id
     */
    @NotBlank(message = "Provider identifier is required")
    private String providerId;

    /**
     * SIP ID
     */
    private String sipId;

    /**
     * State determined through different storage steps
     */
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
    public Optional<String> getSipId() {
        return Optional.ofNullable(sipId);
    }

    public Optional<UniformResourceName> getSipIdUrn() {
        if (sipId == null) {
            return Optional.empty();
        }
        return Optional.of(UniformResourceName.fromString(sipId));
    }

    /**
     * Set the sip id
     * @param sipId
     */
    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    public void setSipId(UniformResourceName sipId) {
        if (sipId != null) {
            sipId.toString();
        }
        this.sipId = null;
    }

    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }

    /**
     * @return the session identifier linked to this AIP
     */
    public String getSession() {
        return this.getProperties().getPdi().getProvenanceInformation().getSession();
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
