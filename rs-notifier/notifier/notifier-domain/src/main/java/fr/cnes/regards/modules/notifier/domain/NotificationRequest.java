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
package fr.cnes.regards.modules.notifier.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.OffsetDateTime;

import org.hibernate.annotations.Type;

import com.google.gson.JsonElement;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;

/**
 * Entity to store notification action
 * @author Kevin Marchois
 *
 */
@Entity
@Table(name = "t_notification_request")
public class NotificationRequest {

    @Id
    @SequenceGenerator(name = "notificationSequence", initialValue = 1, sequenceName = "seq_notification_action")
    @GeneratedValue(generator = "notificationSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "request_id")
    private String requestId;

    @Column(columnDefinition = "jsonb", name = "payload", nullable = false)
    @Type(type = "jsonb")
    private JsonElement payload;

    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    @Type(type = "jsonb")
    private JsonElement metadata;

    /** creation date of the instance */
    @Column(name = "request_date", nullable = false)
    private OffsetDateTime requestDate;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationState state;

    public NotificationRequest(JsonElement payload, JsonElement metadata, String requestId, OffsetDateTime requestDate,
            NotificationState state) {
        this.payload = payload;
        this.requestDate = requestDate;
        this.metadata = metadata;
        this.state = state;
        this.requestId = requestId;
    }

    public NotificationRequest() {
    }

    public JsonElement getPayload() {
        return payload;
    }

    public void setPayload(JsonElement payload) {
        this.payload = payload;
    }

    public JsonElement getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonElement metadata) {
        this.metadata = metadata;
    }

    public OffsetDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(OffsetDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NotificationState getState() {
        return state;
    }

    public void setState(NotificationState state) {
        this.state = state;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
