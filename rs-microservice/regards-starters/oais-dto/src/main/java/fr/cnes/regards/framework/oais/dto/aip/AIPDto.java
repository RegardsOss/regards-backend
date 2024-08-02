/*

 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.oais.dto.aip;

import fr.cnes.regards.framework.oais.dto.AbstractInformationPackage;
import fr.cnes.regards.framework.oais.dto.InformationPackageProperties;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.Assert;

import jakarta.validation.constraints.NotBlank;
import java.util.Optional;

public class AIPDto extends AbstractInformationPackage<OaisUniformResourceName> {

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
     * AIP Version
     */
    private Integer version;

    /**
     * Default constructor
     */
    public AIPDto() {
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
        if ((o == null) || (getClass() != o.getClass())) {
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
     * Build a new {@link AIPDto}
     *
     * @param type       {@link EntityType}
     * @param aipId      AIP URN
     * @param sipId      SIP URN
     * @param providerId the provider id
     */
    public static AIPDto build(EntityType type,
                               OaisUniformResourceName aipId,
                               Optional<UniformResourceName> sipId,
                               String providerId,
                               Integer version) {
        Assert.notNull(type, "Entity type is required.");
        Assert.notNull(aipId, "Uniform resource Name is required.");
        Assert.notNull(providerId, "Provider id is required.");
        AIPDto aip = new AIPDto().withIdAndType(aipId, type);
        aip.setSipId(sipId.orElse(null));
        aip.setProviderId(providerId);
        aip.setVersion(version);
        return aip;
    }

    /**
     * Build a new AIP from SIP properties
     *
     * @param sip        source SIP
     * @param aipId      AIP URN
     * @param sipId      SIP URN
     * @param providerId the provider id
     */
    public static AIPDto build(SIPDto sip,
                               OaisUniformResourceName aipId,
                               Optional<UniformResourceName> sipId,
                               String providerId,
                               Integer version) {
        Assert.notNull(sip, "Valid SIP is required.");
        Assert.notNull(aipId, "Uniform resource Name is required.");
        Assert.notNull(providerId, "Provider id is required.");
        AIPDto aip = new AIPDto().withIdAndType(aipId, sip.getIpType());
        aip.setSipId(sipId.orElse(null));
        aip.setProviderId(providerId);
        aip.setBbox(sip.getBbox().orElse(null));
        aip.setCrs(sip.getCrs().orElse(null));
        aip.setGeometry(sip.getGeometry());
        // Propagate properties from SIP
        aip.setProperties(sip.getProperties());
        aip.setVersion(version);
        return aip;
    }

    @Schema(implementation = InformationPackageProperties.class)
    @Override
    public InformationPackageProperties getProperties() {
        return super.getProperties();
    }
}
