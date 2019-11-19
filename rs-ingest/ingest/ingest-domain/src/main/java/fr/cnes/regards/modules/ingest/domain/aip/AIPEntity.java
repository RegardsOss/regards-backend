/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.AbstractOAISEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Entity
@Table(name = "t_aip",
        indexes = { @Index(name = "idx_aip_id", columnList = "id,aip_id,sip_id"),
            @Index(name = "idx_search_aip", columnList = "session_owner,session_name,state,last_update"),
            @Index(name = "idx_aip_provider_id", columnList = "provider_id"),
            @Index(name = "idx_aip_storage", columnList = "storages"),
            @Index(name = "idx_aip_tags", columnList = "tags"),
            @Index(name = "idx_aip_categories", columnList = "categories"),
            @Index(name = "idx_aip_state", columnList = "state"), })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AIPEntity extends AbstractOAISEntity {

    @Id
    @SequenceGenerator(name = "AipSequence", initialValue = 1, sequenceName = "seq_aip")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AipSequence")
    private Long id;

    /**
     * The AIP Internal identifier (generated URN)
     * versions
     */
    @NotBlank(message = "AIP URN is required")
    @Column(name = "aip_id", length = SIPEntity.MAX_URN_SIZE)
    private String aipId;

    /**
     * The SIP identifier which generate the current AIP
     */
    @NotNull(message = "Related SIP entity is required")
    @ManyToOne
    @JoinColumn(name = "sip_id", foreignKey = @ForeignKey(name = "fk_sip"))
    private SIPEntity sip;

    @NotNull(message = "AIP state is required")
    @Enumerated(EnumType.STRING)
    private AIPState state;

    @NotNull(message = "RAW JSON AIP is required")
    @Column(columnDefinition = "jsonb", name = "rawaip", nullable = false)
    @Type(type = "jsonb")
    private AIP aip;

    /**
     * Real AIP content checksum, computed once all associated files are stored
     */
    @Column(name = "checksum", length = SIPEntity.CHECKSUM_MAX_LENGTH)
    private String checksum;

    /**
     * Manifest locations (the manifest itself can be stored at several locations)
     */
    @Valid
    @Column(columnDefinition = "jsonb", name = "manifest_locations", nullable = false)
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE,
            value = "fr.cnes.regards.framework.oais.OAISDataObjectLocation") })
    private Set<OAISDataObjectLocation> manifestLocations = new HashSet<>();

    /**
     * Storage lists used by this AIP to store its files
     */
    @Column(columnDefinition = "jsonb", name = "storages", nullable = false)
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> storages = new HashSet<>();

    public Long getId() {
        return id;
    }

    public SIPEntity getSip() {
        return sip;
    }

    public void setSip(SIPEntity sip) {
        this.sip = sip;
    }

    public AIPState getState() {
        return state;
    }

    public void setState(AIPState state) {
        this.state = state;
    }

    public AIP getAip() {
        return aip;
    }

    public void setAip(AIP aip) {
        this.aip = aip;
    }

    public String getAipId() {
        return aipId;
    }

    public void setAipId(String aipId) {
        this.aipId = aipId;
    }

    public UniformResourceName getAipIdUrn() {
        return UniformResourceName.fromString(aipId);
    }

    public void setAipId(UniformResourceName aipId) {
        this.aipId = aipId.toString();
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Set<OAISDataObjectLocation> getManifestLocations() {
        return manifestLocations;
    }

    public void setManifestLocations(Set<OAISDataObjectLocation> manifestLocations) {
        this.manifestLocations = manifestLocations;
    }

    public Set<String> getStorages() {
        return storages;
    }

    public void setStorages(Set<String> storages) {
        this.storages = storages;
    }

    public static AIPEntity build(SIPEntity sip, AIPState state, AIP aip) {
        AIPEntity aipEntity = new AIPEntity();
        aipEntity.setAip(aip);
        aipEntity.setState(state);
        aipEntity.setSip(sip);
        aipEntity.setAipId(aip.getId());
        aipEntity.setCreationDate(OffsetDateTime.now());
        aipEntity.setLastUpdate(aipEntity.getCreationDate());
        // Extracted from SIP
        aipEntity.setProviderId(sip.getProviderId());
        aipEntity.setSessionOwner(sip.getSessionOwner());
        aipEntity.setSession(sip.getSession());
        aipEntity.setCategories(sip.getCategories());

        // Extracted from AIP for search purpose
        aipEntity.setTags(new HashSet<>(aip.getTags()));
        return aipEntity;
    }
}
