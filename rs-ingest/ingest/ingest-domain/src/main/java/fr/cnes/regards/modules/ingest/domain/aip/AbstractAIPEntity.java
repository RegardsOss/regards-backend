/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.modules.ingest.domain.AbstractOAISEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;

/**
 * AIP can be retrieve with two ways.
 * <ul>
 * <li>Getting full AIPEntity with SIP thanks to {@link AIPEntity}</li>
 * <li>Getting light AIPEntity without SIP thanks to {@link AIPEntityLight}</li>
 * </ul>
 *
 * This abstract class represents the common part between the two pojos.
 *
 * @author Sébastien Binda
 *
 */
@MappedSuperclass
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public abstract class AbstractAIPEntity extends AbstractOAISEntity {

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

    @NotNull(message = "AIP state is required")
    @Enumerated(EnumType.STRING)
    private AIPState state;

    @NotNull(message = "RAW JSON AIP is required")
    @Column(columnDefinition = "jsonb", name = "rawaip", nullable = false)
    @Type(type = "jsonb")
    private AIP aip;

    /**
     * Storage lists used by this AIP to store its files
     */
    @Column(columnDefinition = "jsonb", name = "storages", nullable = false)
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> storages = new HashSet<>();

    @Column
    private boolean last = false;

    public Long getId() {
        return id;
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

    public OaisUniformResourceName getAipIdUrn() {
        return OaisUniformResourceName.fromString(aipId);
    }

    public void setAipId(OaisUniformResourceName aipId) {
        this.aipId = aipId.toString();
    }

    public Set<String> getStorages() {
        return storages;
    }

    public void setStorages(Set<String> storages) {
        this.storages = storages;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public static AIPEntity build(AIPState state, AIP aip) {
        AIPEntity aipEntity = new AIPEntity();
        aipEntity.setAip(aip);
        aipEntity.setState(state);
        aipEntity.setAipId(aip.getId());
        aipEntity.setCreationDate(OffsetDateTime.now());
        aipEntity.setLastUpdate(aipEntity.getCreationDate());

        // Extracted from AIP for search purpose
        aipEntity.setTags(new HashSet<>(aip.getTags()));
        return aipEntity;
    }

}
