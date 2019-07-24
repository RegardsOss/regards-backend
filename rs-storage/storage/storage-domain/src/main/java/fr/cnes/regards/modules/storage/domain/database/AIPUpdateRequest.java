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
package fr.cnes.regards.modules.storage.domain.database;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * POJO to handle AIP update request. Those request exists if an update request is applied to an {@link AIPEntity} in
 * storing process (state != STORED). Only the last update request on an AIP is saved.
 * Thoses requests are handle by a scheduler.
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_aip_update_req", indexes = { @Index(name = "idx_aip_update_req_ip_id", columnList = "aip_id") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_aip_update_req_ipId", columnNames = "aip_id") })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AIPUpdateRequest {

    /**
     * The id
     */
    @Id
    @SequenceGenerator(name = "AipUpdateSequence", initialValue = 1, sequenceName = "seq_aip_update_req")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AipUpdateSequence")
    private Long id;

    /**
     * private Id for the application, it's a {@link UniformResourceName} but due to the need of retrieving all AIP's
     * version(which is in {@link UniformResourceName}) it's mapped to a String, validated as a URN
     */
    @Column(name = "aip_id", length = AIPEntity.MAX_URN_SIZE)
    private String aipId;

    /**
     * The actual aip metadata
     */
    @Column(columnDefinition = "jsonb", name = "json_aip")
    @Type(type = "jsonb")
    private AIP aip;

    @Column(name = "update_message", length = 512)
    private String updateMessage;

    public AIPUpdateRequest() {

    }

    public AIPUpdateRequest(AIP aip, String updateMessage) {
        this.aipId = aip.getId().toString();
        this.aip = aip;
        this.updateMessage = updateMessage;
    }

    public String getUpdateMessage() {
        return updateMessage;
    }

    public void setUpdateMessage(String updateMessage) {
        this.updateMessage = updateMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAipId() {
        return aipId;
    }

    public void setAipId(String aipId) {
        this.aipId = aipId;
    }

    public AIP getAip() {
        return aip;
    }

    public void setAip(AIP aip) {
        this.aip = aip;
    }

}
