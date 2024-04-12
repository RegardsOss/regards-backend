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
package fr.cnes.regards.modules.feature.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationInfoType;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationRequest;

import javax.persistence.*;
import java.time.OffsetDateTime;

/**
 * Store info about a feature dissemination :
 * <ul>
 *     <li>the system that received the feature, with its name (which is an identifier)</li>
 *     <li>when the recipient has been contacted</li>
 *     <li>when it acknowledges</li>
 *     <li>when it is blocking</li>
 * </ul>
 *
 * @author Léo Mieulet
 */
@Entity
@Table(name = "t_feature_dissemination_info")
public class FeatureDisseminationInfo {

    @Id
    @SequenceGenerator(name = "featureDisseminationInfoSequence",
                       initialValue = 1,
                       sequenceName = "seq_feature_dissemination_info")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "featureDisseminationInfoSequence")
    protected Long id;

    /**
     * The name of the broadcast recipient
     */
    @Column(name = "label", length = 128, nullable = false)
    private String label;

    /**
     * Diffusion date
     */
    @Column(name = "request_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime requestDate;

    /**
     * Date of acknowledge.
     * This date is updated to indicate the recipient has acknowledged the diffusion.
     */
    @Column(name = "ack_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime ackDate;

    @Column(name = "feature_id")
    private Long featureId;

    @Column(name = "blocking")
    private boolean blocking;

    /**
     * Constructor
     */
    public FeatureDisseminationInfo() {
    }

    /**
     * Constructor
     * <p>
     * Update the date of acknowledge using provided ackRequired :
     * <ul>
     * <li>When true, the date of acknowledge will be specified when the recipient notifies us</li>
     * <li>When false, we won't receive a acknowledgement, so not blocking</li>
     * </ul>
     *
     * @param FeatureUpdateDisseminationRequest the request origin of the new Dissemination info object
     */
    public FeatureDisseminationInfo(FeatureUpdateDisseminationRequest disseminationRequest) {
        this.requestDate = disseminationRequest.getCreationDate();
        this.label = disseminationRequest.getRecipientLabel();
        if (disseminationRequest.getUpdateType() == FeatureUpdateDisseminationInfoType.PUT
            && disseminationRequest.getAckRequired()) {
            this.ackDate = null;
        } else {
            this.ackDate = this.requestDate;
        }
        this.blocking = disseminationRequest.isBlockingRequired();
    }

    /**
     * Updates dissemination info object for response of PUT request.
     * Logic is complex cause of possible async issue in put and ack response order.
     * Main issue is encountered when ack response is handled before put response
     */
    public void updateRequestDate(OffsetDateTime disseminationDate, boolean ackRequired) {
        // Nominal case, both dissemination init and ack dates are null
        if (isNew()) {
            this.requestDate = disseminationDate;
            this.ackDate = ackRequired ? null : disseminationDate;
            return;
        }
        // Nominal case of re-notification, dissemination init date is set and ack date is null
        if (isWaitingAck() && this.requestDate.isBefore(disseminationDate)) {
            // Update init date, only if new init date is after the current init date
            this.requestDate = disseminationDate;
            this.ackDate = ackRequired ? null : disseminationDate;
            return;
        }
        // Nominal case of re-notification after acknowledged
        if (isAcknowledged() && this.requestDate.isBefore(disseminationDate)) {
            // Update init date, only if new init date is after the current init date
            this.requestDate = disseminationDate;
            this.ackDate = ackRequired ? null : disseminationDate;
        }
    }

    /**
     * Updates dissemination info object for response of ACK request.
     * Logic is complex cause of possible async issue in PUT and ack response order.
     * Main issue is encountered when ACK response is handled before PUT response
     */
    public void updateAckDate(OffsetDateTime ackDate) {
        // Async issue case, ack arrive before init date
        if (isNew()) {
            this.requestDate = ackDate;
            this.ackDate = ackDate;
            return;
        }
        // Nominal case, ack after init date
        if (isWaitingAck() && ackDate.isAfter(this.requestDate)) {
            this.ackDate = ackDate;
        }
        // Async issue re-notification case, new ack date arrives before new init date.
        if (isAcknowledged() && ackDate.isAfter(this.ackDate)) {
            this.requestDate = ackDate;
            this.ackDate = ackDate;
        }
    }

    /**
     * Check if current dissemination information is new. Both request and ack dates are null
     */
    public boolean isNew() {
        return this.ackDate == null && this.requestDate == null;
    }

    /**
     * Check if current dissemination information is waiting for acknowledge
     */
    public boolean isWaitingAck() {
        return this.requestDate != null && this.ackDate == null;
    }

    /**
     * Check if current dissemination information is completed. Request and acknowledged dates are set and ack is
     * after request date.
     */
    public boolean isAcknowledged() {
        return this.requestDate != null && this.ackDate != null && (this.ackDate.isEqual(this.requestDate)
                                                                    || this.ackDate.isAfter(this.requestDate));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public OffsetDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(OffsetDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public OffsetDateTime getAckDate() {
        return ackDate;
    }

    public void setAckDate(OffsetDateTime ackDate) {
        this.ackDate = ackDate;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public boolean isAckPending() {
        return getAckDate() == null;
    }

    @Override
    public String toString() {
        return "FeatureDisseminationInfo{"
               + "id="
               + id
               + ", label='"
               + label
               + '\''
               + ", requestDate="
               + requestDate
               + ", ackDate="
               + ackDate
               + ", blocking="
               + blocking
               + '}';
    }

    public Long getFeatureId() {
        return featureId;
    }
}
