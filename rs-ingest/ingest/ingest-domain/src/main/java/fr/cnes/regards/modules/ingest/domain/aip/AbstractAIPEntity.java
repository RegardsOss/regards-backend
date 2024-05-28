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

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.modules.ingest.domain.AbstractOAISEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.dto.DisseminationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AIP can be retrieve with two ways.
 * <ul>
 * <li>Getting full AIPEntity with SIP thanks to {@link AIPEntity}</li>
 * <li>Getting light AIPEntity without SIP thanks to {@link AIPEntityLight}</li>
 * </ul>
 * <p>
 * This abstract class represents the common part between the two pojos.
 *
 * @author Sébastien Binda
 */
@MappedSuperclass

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
    @Type(JsonBinaryType.class)
    private AIPDto aip;

    /**
     * Storage lists used by this AIP to store its files
     */
    @Column(columnDefinition = "jsonb", name = "storages", nullable = false)
    @Type(value = JsonBinaryType.class,
          parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> storages = new HashSet<>();

    /**
     * Optional parameter showing that the entity come from another entity in an external catalog.
     * The origin urn is the urn of the external entity.
     */
    @Column(name = "origin_urn", length = SIPEntity.MAX_URN_SIZE)
    private String originUrn;

    @Column
    private boolean last = false;

    @Column(name = "dissemination_infos", columnDefinition = "jsonb")
    @Type(value = JsonBinaryType.class,
          parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE,
                                    value = "fr.cnes.regards.modules.ingest.domain.aip.DisseminationInfo") })
    private List<DisseminationInfo> disseminationInfos;

    @NotNull
    @Column(name = "dissemination_status", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private DisseminationStatus disseminationStatus = DisseminationStatus.NONE;

    public Long getId() {
        return id;
    }

    public AIPState getState() {
        return state;
    }

    public void setState(AIPState state) {
        this.state = state;
    }

    public AIPDto getAip() {
        return aip;
    }

    public void setAip(AIPDto aip) {
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

    public String getOriginUrn() {
        return originUrn;
    }

    public void setOriginUrn(String originUrn) {
        this.originUrn = originUrn;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public List<DisseminationInfo> getDisseminationInfos() {
        return disseminationInfos;
    }

    public void setDisseminationInfos(List<DisseminationInfo> disseminationInfos) {
        this.disseminationInfos = disseminationInfos;
    }

    public static AIPEntity build(AIPState state, AIPDto aip) {
        AIPEntity aipEntity = new AIPEntity();
        aipEntity.setAip(aip);
        aipEntity.setState(state);
        aipEntity.setAipId(aip.getId());
        aipEntity.setCreationDate(OffsetDateTime.now());
        aipEntity.setOriginUrn(aip.getProperties().getPdi().getProvenanceInformation().getOriginUrn());
        aipEntity.setLastUpdate(aipEntity.getCreationDate());
        aipEntity.setDisseminationStatus(DisseminationStatus.NONE);

        // Extracted from AIP for search purpose
        aipEntity.setTags(new HashSet<>(aip.getTags()));
        return aipEntity;
    }

    public DisseminationStatus getDisseminationStatus() {
        return disseminationStatus;
    }

    public void setDisseminationStatus(DisseminationStatus disseminationStatus) {
        this.disseminationStatus = disseminationStatus;
    }
}
