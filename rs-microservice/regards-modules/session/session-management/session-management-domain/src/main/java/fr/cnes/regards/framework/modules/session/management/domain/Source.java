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
package fr.cnes.regards.framework.modules.session.management.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * A source represent the current states of all related sessions
 *
 * @author Iliana Ghazali
 **/
@Entity
@Table(name = "t_source_manager")
public class Source {

    /**
     * Name of the source
     */
    @Id
    @NotNull
    @Column(name="name")
    private String name;

    /**
     * Number of sessions in the source
     */
    @Column(name="nb_sessions")
    @NotNull
    private long nbSessions = 0L;

    /**
     * Set of SourceStepAggregation associated to this source
     */
    @Valid
    @OneToMany(fetch= FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "source_name", foreignKey = @ForeignKey(name = "fk_source_step_aggregation"))
    private Set<SourceStepAggregation> steps = new HashSet<>();

    /**
     * Date when source was last updated
     */
    @Column(name = "last_update_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdateDate;

    @Embedded
    private ManagerState managerState = new ManagerState();

    public Source(@NotNull String name) {
        this.name = name;
    }

    public Source(){
    }

    public String getName() {
        return name;
    }

    public long getNbSessions() {
        return nbSessions;
    }

    public void setNbSessions(long nbSessions) {
        this.nbSessions = nbSessions;
    }

    public Set<SourceStepAggregation> getSteps() {
        return steps;
    }

    public void setSteps(Set<SourceStepAggregation> pSteps) {
        // This method is used to prevent the override of the set that Hibernate is tracking
        this.steps.clear();
        if(pSteps != null) {
            this.steps.addAll(pSteps);
        }
    }

    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(OffsetDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public ManagerState getManagerState() {
        return managerState;
    }

    public void setManagerState(ManagerState managerState) {
        this.managerState = managerState;
    }
}
