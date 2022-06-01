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
package fr.cnes.regards.framework.modules.session.manager.domain;

import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Corresponding steps of a source. Each source has X step among {@link StepTypeEnum} types.
 *
 * @author Iliana Ghazali
 **/
@Entity
@Table(name = "t_source_step_aggregation")
public class SourceStepAggregation {

    /**
     * Id of the SourceStepAggregation
     */
    @Id
    @SequenceGenerator(name = "sourceAggSequence", initialValue = 1, sequenceName = "seq_source_agg")
    @GeneratedValue(generator = "sourceAggSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Type of session step
     */
    @Column(name = "type")
    @NotNull
    @Enumerated(value = EnumType.STRING)
    private StepTypeEnum type;

    /**
     * Number of events related to inputs
     */
    @Column(name = "total_in")
    @NotNull
    private long totalIn = 0L;

    /**
     * Number of events related to outputs
     */
    @Column(name = "total_out")
    @NotNull
    private long totalOut = 0L;

    /**
     * Number of requests in ERROR or WAITING mode and if process is RUNNING
     */
    @Column(name = "state")
    @NotNull
    @Embedded
    private AggregationState state = new AggregationState();

    public SourceStepAggregation(StepTypeEnum type) {
        this.type = type;
    }

    public SourceStepAggregation() {
    }

    public StepTypeEnum getType() {
        return type;
    }

    public void setType(StepTypeEnum type) {
        this.type = type;
    }

    public long getTotalIn() {
        return totalIn;
    }

    public void setTotalIn(long totalIn) {
        this.totalIn = totalIn;
    }

    public long getTotalOut() {
        return totalOut;
    }

    public void setTotalOut(long totalOut) {
        this.totalOut = totalOut;
    }

    public AggregationState getState() {
        return state;
    }

    public void setState(AggregationState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "SourceStepAggregation{"
               + "id="
               + id
               + ", type="
               + type
               + ", totalIn="
               + totalIn
               + ", totalOut="
               + totalOut
               + ", state="
               + state
               + '}';
    }
}
