/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.domain;

import java.time.OffsetDateTime;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

/**
 * Register an acquisition chain execution
 * @author Christophe Mertz
 */
@Entity
@Table(name = "t_acquisition_exec_chain", indexes = { @Index(name = "idx_acq_exec_session", columnList = "session") },
        uniqueConstraints = @UniqueConstraint(name = "uk_acq_exec_session", columnNames = { "session" }))
public class ExecAcquisitionProcessingChain implements IIdentifiable<Long> {

    /**
     * A constant used to define a {@link String} constraint with length 255
     */
    private static final int MAX_STRING_LENGTH = 255;

    /**
     * entity id for SGBD purpose mainly and REST request
     */
    @Id
    @SequenceGenerator(name = "ExecSequence", initialValue = 1, sequenceName = "seq_exec_chain")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ExecSequence")
    private Long id;

    /**
     * If a {@link ExecAcquisitionProcessingChain} is running, the current session identifier must be defined and unique
     */
    @Column(length = MAX_STRING_LENGTH)
    private String session;

    /**
     * The {@link AcquisitionProcessingChain} associate to the current {@link ExecAcquisitionProcessingChain}
     */
    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "chain_id", foreignKey = @ForeignKey(name = "fk_acq_exec_chain_id"), updatable = false)
    private AcquisitionProcessingChain chainGeneration;

    /**
     * The start date of the {@link ExecAcquisitionProcessingChain}
     */
    @Column(name = "start_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime startDate;

    /**
     * The stop date of the {@link ExecAcquisitionProcessingChain}
     */
    @Column(name = "stop_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime stopDate;

    @Override
    public Long getId() {
        return id;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public AcquisitionProcessingChain getChainGeneration() {
        return chainGeneration;
    }

    public void setChainGeneration(AcquisitionProcessingChain chainGeneration) {
        this.chainGeneration = chainGeneration;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public OffsetDateTime getStopDate() {
        return stopDate;
    }

    public void setStopDate(OffsetDateTime stopDate) {
        this.stopDate = stopDate;
    }

    @Override
    public int hashCode() { // NOSONAR
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((id == null) ? 0 : id.hashCode());
        result = (prime * result) + ((session == null) ? 0 : session.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) { // NOSONAR
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ExecAcquisitionProcessingChain other = (ExecAcquisitionProcessingChain) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (session == null) {
            if (other.session != null) {
                return false;
            }
        } else if (!session.equals(other.session)) {
            return false;
        }
        return true;
    }

}
