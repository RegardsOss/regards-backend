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
package fr.cnes.regards.modules.ingest.domain.entity.request;

import javax.persistence.Column;
import javax.persistence.Embedded;
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
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.entity.IngestMetadata;

/**
 *
 * Each SIP received by message broker results in an {@link IngestRequest}
 *
 * @author Marc SORDI
 *
 */
@Entity
@Table(name = "t_ingest_request", indexes = { @Index(name = "idx_ingest_request_id", columnList = "request_id") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_ingest_request_id", columnNames = { "request_id" }) })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class IngestRequest {

    @Id
    @SequenceGenerator(name = "ingestRequestSequence", initialValue = 1, sequenceName = "seq_ingest_request")
    @GeneratedValue(generator = "ingestRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "request_id", length = 36, nullable = false, updatable = false)
    private String requestId;

    @Embedded
    private IngestMetadata metadata;

    @Column(columnDefinition = "jsonb", name = "rawsip")
    @Type(type = "jsonb")
    private SIP sip;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IngestMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(IngestMetadata metadata) {
        this.metadata = metadata;
    }

    public SIP getSip() {
        return sip;
    }

    public void setSip(SIP sip) {
        this.sip = sip;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public static IngestRequest build(String requestId, IngestMetadata metadata, SIP sip) {
        IngestRequest request = new IngestRequest();
        request.setRequestId(requestId);
        request.setMetadata(metadata);
        request.setSip(sip);
        return request;
    }
}
