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
package fr.cnes.regards.modules.workermanager.domain.request;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.workermanager.dto.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestDTO;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import org.hibernate.annotations.Type;
import org.springframework.amqp.core.Message;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * WorkerRequest : Requests to be dispatched to workers by the WorkerManager
 *
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_workermanager_request", indexes = { @Index(name = "idx_worker_request_id", columnList = "request_id"),
    @Index(name = "idx_worker_request_content_type", columnList = "content_type") }, uniqueConstraints = {
    @UniqueConstraint(name = "uk_t_workermanager_request_requestid", columnNames = { "request_id" }) })
public class Request {

    @Id
    @SequenceGenerator(name = "workerRequestSequence", initialValue = 1, sequenceName = "worker_request_sequence")
    @GeneratedValue(generator = "workerRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "creation_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime creationDate;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "session", nullable = false)
    private String session;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @Column(name = "dispatched_worker_type")
    private String dispatchedWorkerType;

    @Column(name = "content", nullable = false)
    @Type(type = "org.hibernate.type.BinaryType")
    @Lob
    private byte[] content;

    @Column(name = "error")
    @Type(type = "text")
    private String error;

    public Request() {
    }

    public Request(Message message, RequestStatus status) {
        this.requestId = EventHeadersHelper.getRequestIdHeader(message).get();
        this.creationDate = OffsetDateTime.now();
        this.contentType = EventHeadersHelper.getContentTypeHeader(message).get();
        this.source = EventHeadersHelper.getOwnerHeader(message).get();
        this.session = EventHeadersHelper.getSessionHeader(message).get();
        this.status = status;
        this.content = message.getBody();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getDispatchedWorkerType() {
        return dispatchedWorkerType;
    }

    public void setDispatchedWorkerType(String dispatchedWorkerType) {
        this.dispatchedWorkerType = dispatchedWorkerType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Request that = (Request) o;
        return getRequestId().equals(that.getRequestId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRequestId());
    }

    public RequestDTO toDTO() {
        return new RequestDTO(this.requestId,
                              this.creationDate,
                              this.contentType,
                              this.source,
                              this.session,
                              this.status,
                              this.dispatchedWorkerType,
                              this.error);
    }
}
