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
package fr.cnes.regards.modules.ingest.domain.request.update;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * @author Léo Mieulet
 */
@Entity
@Table(name = "t_update_task")
@DiscriminatorColumn(name = "dtype", length = 16)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class AIPUpdateTask {

    @Id
    @SequenceGenerator(name = "aipUpdateTaskSequence", initialValue = 1, sequenceName = "seq_aip_update_task")
    @GeneratedValue(generator = "aipUpdateTaskSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * The AIP Internal identifier (generated URN)
     */
    @NotBlank(message = "AIP URN is required")
    @Column(name = "aip_id", length = SIPEntity.MAX_URN_SIZE)
    private String aipId;

    @NotNull(message = "AIP update state is required")
    @Enumerated(EnumType.STRING)
    private AIPUpdateState state;

    @NotNull(message = "Update task type is required")
    @Enumerated(EnumType.STRING)
    private AIPUpdateTaskType type;

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

    public AIPUpdateState getState() {
        return state;
    }

    public void setState(AIPUpdateState state) {
        this.state = state;
    }

    public AIPUpdateTaskType getType() {
        return type;
    }

    public void setType(AIPUpdateTaskType type) {
        this.type = type;
    }
}
