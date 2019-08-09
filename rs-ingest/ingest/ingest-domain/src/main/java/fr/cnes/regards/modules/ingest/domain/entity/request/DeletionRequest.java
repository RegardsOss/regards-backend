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
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;

/**
 * @author Marc SORDI
 */
@Entity
@Table(name = "t_deletion_request")
public class DeletionRequest {

    @Id
    @SequenceGenerator(name = "deletionRequestSequence", initialValue = 1, sequenceName = "seq_deletion_request")
    @GeneratedValue(generator = "deletionRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * The SIP internal identifier (generated URN).
     */
    @NotBlank(message = "SIP ID is required")
    @Column(name = "sipId", length = SIPEntity.MAX_URN_SIZE)
    private String sipId;

}
