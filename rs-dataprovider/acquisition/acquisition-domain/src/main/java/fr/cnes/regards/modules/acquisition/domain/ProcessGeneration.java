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
 * 
 * @author Christophe Mertz
 */
@Entity
@Table(name = "t_acquisition_process", indexes = { @Index(name = "idx_acq_process_session", columnList = "session") },
        uniqueConstraints = @UniqueConstraint(name = "uk_acq_process_session", columnNames = { "session" }))
public class ProcessGeneration implements IIdentifiable<Long> {

    /**
     * A constant used to define a {@link String} constraint with length 256
     */
    private static final int MAX_STRING_LENGTH = 256;

    /**
     * entity id for SGBD purpose mainly and REST request
     */
    @Id
    @SequenceGenerator(name = "ProcessSequence", initialValue = 1, sequenceName = "seq_process")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ProcessSequence")
    private Long id;

    /**
     * If a {@link ProcessGeneration} is running, the current session identifier must be defined and unique
     */
    @Column(length = MAX_STRING_LENGTH)
    private String session;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "chain_id", referencedColumnName = "ID", foreignKey = @ForeignKey(name = "fk_acq_chain_id"),
            updatable = false)
    private ChainGeneration chainGeneration;

    @Column(name = "start_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime startDate;

    @Column(name = "stop_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime stopDate;

    @Column(name = "nb_sip_created", nullable = false)
    private int nbSipCreated = 0;

    @Column(name = "nb_sip_in_error", nullable = false)
    private int nbSipError = 0;

    @Column(name = "nb_sip_stored", nullable = false)
    private int nbSipStored = 0;

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

    public ChainGeneration getChainGeneration() {
        return chainGeneration;
    }

    public void setChainGeneration(ChainGeneration chainGeneration) {
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

    public int getNbSipCreated() {
        return nbSipCreated;
    }

    public void setNbSipCreated(int nbSipCreated) {
        this.nbSipCreated = nbSipCreated;
    }

    public void sipCreatedIncrease() {
        this.nbSipCreated++;
    }

    public int getNbSipError() {
        return nbSipError;
    }

    public void setNbSipError(int nbSipError) {
        this.nbSipError = nbSipError;
    }

    public void sipErrorIncrease() {
        this.nbSipError++;
    }

    public int getNbSipStored() {
        return nbSipStored;
    }

    public void setNbSipStored(int nbSipStored) {
        this.nbSipStored = nbSipStored;
    }

    public void sipStoredIncrease() {
        this.nbSipStored++;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((chainGeneration == null) ? 0 : chainGeneration.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((session == null) ? 0 : session.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProcessGeneration other = (ProcessGeneration) obj;
        if (chainGeneration == null) {
            if (other.chainGeneration != null)
                return false;
        } else if (!chainGeneration.equals(other.chainGeneration))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (session == null) {
            if (other.session != null)
                return false;
        } else if (!session.equals(other.session))
            return false;
        return true;
    }

}
