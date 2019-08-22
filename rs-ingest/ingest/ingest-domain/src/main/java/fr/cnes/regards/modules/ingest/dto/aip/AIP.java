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
package fr.cnes.regards.modules.ingest.dto.aip;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotBlank;

import org.springframework.util.Assert;

import fr.cnes.regards.framework.oais.AbstractInformationPackage;
import fr.cnes.regards.framework.oais.urn.EntityType;
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
     * Default constructor
     */
    public AIP() {
        super();
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
            this.sipId = sipId.toString();
        } else {
            this.sipId = null;
        }
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    // Fluent API

    /**
     * Build a new {@link AIP}
     * @param type {@link EntityType}
     * @param aipId AIP URN
     * @param sipId SIP URN
     * @param providerId the provider id
     * @param categories context configuration categories
     */
    public static AIP build(EntityType type, UniformResourceName aipId, Optional<UniformResourceName> sipId,
            String providerId, List<String> categories) {
        Assert.notNull(type, "Entity type is required.");
        Assert.notNull(aipId, "Uniform resource Name is required.");
        Assert.notEmpty(categories, "At least one category is required");
        AIP aip = new AIP().withIdAndType(aipId, type)
                .withContextCategories(categories.toArray(new String[categories.size()]));
        aip.setSipId(sipId.orElse(null));
        aip.setProviderId(providerId);
        return aip;
    }
}
