/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

@Entity
@Table(name = "t_last_aip")
public class LastAIPEntity {

    @Id
    @SequenceGenerator(name = "LastAipSequence", initialValue = 1, sequenceName = "seq_last_aip")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LastAipSequence")
    private Long id;

    @Column(name = "aip_id", nullable = false)
    private Long aipId;

    @NotBlank(message = "Provider ID is required")
    @Column(name = "provider_id", length = 255, nullable = false)
    private String providerId;

    public LastAIPEntity() {
        // Default constructor
    }

    public LastAIPEntity(Long aipId, String providerId) {
        this.aipId = aipId;
        this.providerId = providerId;
    }

    public Long getAipId() {
        return aipId;
    }

    public void setAipId(Long aipId) {
        this.aipId = aipId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
