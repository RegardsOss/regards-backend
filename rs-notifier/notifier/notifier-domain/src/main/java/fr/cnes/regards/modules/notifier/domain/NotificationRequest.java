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
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.Type;
import org.springframework.util.Assert;

import com.google.gson.JsonElement;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.notifier.domain.plugin.IRuleMatcher;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;

/**
 * Entity to store notification action
 * @author Kevin Marchois
 *
 */
@Entity
@Table(name = "t_notification_request")
public class NotificationRequest {

    /**
     * This is JPQL name of the {@link #requestDate} attribute
     */
    public static final String REQUEST_DATE_JPQL_NAME = "requestDate";

    /**
     * Allows to reflect result of rule matching phase.
     */
    @OneToMany
    @JoinTable(name = "ta_notif_request_recipients_toschedule",
            joinColumns = @JoinColumn(name = "notification_request_id"),
            inverseJoinColumns = @JoinColumn(name = "recipient_id"),
            foreignKey = @ForeignKey(name = "fk_notification_request_id_recipients_toschedule"),
            inverseForeignKey = @ForeignKey(name = "fk_notification_request_recipients_toschedule_id"))
    private final Set<PluginConfiguration> recipientsToSchedule = new HashSet<>();

    /**
     * Allows to know which recipient are being handled. This also allows to know when a notification request has been successfully handled.
     */
    @OneToMany
    @JoinTable(name = "ta_notif_request_recipients_scheduled",
            joinColumns = @JoinColumn(name = "notification_request_id"),
            inverseJoinColumns = @JoinColumn(name = "recipient_id"),
            foreignKey = @ForeignKey(name = "fk_notification_request_id_recipients_scheduled"),
            inverseForeignKey = @ForeignKey(name = "fk_notification_request_recipients_scheduled_id"))
    private final Set<PluginConfiguration> recipientsScheduled = new HashSet<>();

    /**
     * Allows to know which recipient could not be successfully handled. This allows to know which recipient are to retry for a request.
     */
    @OneToMany
    @JoinTable(name = "ta_notif_request_recipients_error", joinColumns = @JoinColumn(name = "notification_request_id"),
            inverseJoinColumns = @JoinColumn(name = "recipient_id"),
            foreignKey = @ForeignKey(name = "fk_notification_request_id_recipients_error"),
            inverseForeignKey = @ForeignKey(name = "fk_notification_request_recipients_error_id"))
    private final Set<PluginConfiguration> recipientsInError = new HashSet<>();

    /**
     * Allows to know which rules are to be matched during matching phase.
     */
    @OneToMany
    @JoinTable(name = "ta_notif_request_recipients_error", joinColumns = @JoinColumn(name = "notification_request_id"),
            inverseJoinColumns = @JoinColumn(name = "recipient_id"),
            foreignKey = @ForeignKey(name = "fk_notification_request_id_recipients_error"),
            inverseForeignKey = @ForeignKey(name = "fk_notification_request_recipients_error_id"))
    private final Set<PluginConfiguration> rulesToMatch = new HashSet<>();

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

    //TODO mapping
    private String requestOwner;

    public NotificationRequest(JsonElement payload, JsonElement metadata, String requestId, String requestOwner,
            OffsetDateTime requestDate, NotificationState state, Set<PluginConfiguration> rulesToMatch) {
        Assert.isTrue(rulesToMatch.stream()
                              .allMatch(rule -> rule.getInterfaceNames().contains(IRuleMatcher.class.getName())),
                      String.format(
                              "While initializing a %s only plugin configuration for type %s are allowed to be given",
                              this.getClass().getSimpleName(),
                              IRuleMatcher.class.getName()));
        this.payload = payload;
        this.requestDate = requestDate;
        this.metadata = metadata;
        this.state = state;
        this.requestId = requestId;
        this.requestOwner = requestOwner;
        this.rulesToMatch.addAll(rulesToMatch);
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

    public Set<PluginConfiguration> getRecipientsInError() {
        return recipientsInError;
    }

    public Set<PluginConfiguration> getRecipientsScheduled() {
        return recipientsScheduled;
    }

    public Set<PluginConfiguration> getRecipientsToSchedule() {
        return recipientsToSchedule;
    }

    public Set<PluginConfiguration> getRulesToMatch() {
        return rulesToMatch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NotificationRequest that = (NotificationRequest) o;
        return Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    public String getRequestOwner() {
        return requestOwner;
    }
}
