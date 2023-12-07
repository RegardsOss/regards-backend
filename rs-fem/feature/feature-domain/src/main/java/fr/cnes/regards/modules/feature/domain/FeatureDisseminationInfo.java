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
     * When broadcast recipient acknowledge the diffusion
     */
    @Column(name = "ack_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime ackDate;

    @Column(name = "blocking")
    private boolean blocking;

    public FeatureDisseminationInfo() {
    }

    /**
     * @param ackRequired when false, no acknowledge message will be received
     */
    public FeatureDisseminationInfo(String label, boolean ackRequired) {
        this.requestDate = OffsetDateTime.now();

        this.label = label;
        this.setAckDateByAckRequired(ackRequired);
    }

    /**
     * Update the field ackDate using provided ackRequired :
     * <ul>
     * <li>When true, the ackDate will be specified when the recipient notifies us</li>
     * <li>When false, we won't receive a acknowledgement, so not blocking</li>
     * </ul>
     */
    public final void setAckDateByAckRequired(boolean ackRequired) {
        if (ackRequired) {
            this.ackDate = null;
        } else {
            this.ackDate = this.requestDate;
        }
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
}
