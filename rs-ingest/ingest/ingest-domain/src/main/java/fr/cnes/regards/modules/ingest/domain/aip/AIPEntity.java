/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.aip;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;

@Entity
@Table(name = "t_aip", indexes = { @Index(name = "idx_aip_id", columnList = "id,aip_id,sip_id"),
        @Index(name = "idx_search_aip", columnList = "session_owner,session_name,state,last_update"),
        @Index(name = "idx_aip_provider_id", columnList = "provider_id"),
        @Index(name = "idx_aip_storage", columnList = "storages"), @Index(name = "idx_aip_tags", columnList = "tags"),
        @Index(name = "idx_aip_categories", columnList = "categories"),
        @Index(name = "idx_aip_sip_id", columnList = "sip_id"), @Index(name = "idx_aip_state", columnList = "state"), })
public class AIPEntity extends AbstractAIPEntity {

    /**
     * The SIP identifier which generate the current AIP
     */
    @NotNull(message = "Related SIP entity is required")
    @ManyToOne
    @JoinColumn(name = "sip_id", foreignKey = @ForeignKey(name = "fk_sip"))
    private SIPEntity sip;

    /**
     * Manifest locations (the manifest itself can be stored at several locations)
     */
    @Valid
    @Column(columnDefinition = "jsonb", name = "manifest_locations", nullable = false)
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE,
            value = "fr.cnes.regards.framework.oais.OAISDataObjectLocation") })
    private Set<OAISDataObjectLocation> manifestLocations = new HashSet<>();

    public Set<OAISDataObjectLocation> getManifestLocations() {
        return manifestLocations;
    }

    public void setManifestLocations(Set<OAISDataObjectLocation> manifestLocations) {
        this.manifestLocations = manifestLocations;
    }

    public SIPEntity getSip() {
        return sip;
    }

    public void setSip(SIPEntity sip) {
        this.sip = sip;
        this.setIpType(sip.getIpType());
    }

    public static AIPEntity build(SIPEntity sip, AIPState state, AIP aip) {
        AIPEntity aipEntity = AbstractAIPEntity.build(state, aip);
        aipEntity.setSip(sip);
        // Extracted from SIP
        aipEntity.setProviderId(sip.getProviderId());
        aipEntity.setSessionOwner(sip.getSessionOwner());
        aipEntity.setSession(sip.getSession());
        aipEntity.setCategories(sip.getCategories());
        aipEntity.setVersion(aip.getVersion());
        return aipEntity;
    }

}
