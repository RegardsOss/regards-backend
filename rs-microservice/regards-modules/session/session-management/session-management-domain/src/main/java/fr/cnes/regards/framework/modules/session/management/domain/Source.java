package fr.cnes.regards.framework.modules.session.management.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import java.time.OffsetDateTime;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
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
@Table(name = "t_source")
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
    private long nbSessions = 0;

    /**
     * Set of SourceStepAggregation associated to this source
     */
    @Valid
    @Column(name = "steps")
    @NotNull(message = "At least one source aggregation is required")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "source", foreignKey = @ForeignKey(name = "fk_source_step_aggregation"))
    private List<SourceStepAggregation> steps;

    /**
     * Date when source was last updated
     */
    @Column(name = "last_update_date")
    @NotNull
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdateDate;

    /**
     * If Source is running, ie, if one of Session is running
     */
    @Column(name = "running")
    @NotNull
    private boolean running = false;

    /**
     * If Source is in error, ie, if one of Session is in error state
     */
    @Column(name = "error")
    @NotNull
    private boolean error = false;

    /**
     * If Source is waiting, ie, if one of Session is in waiting state
     */
    @Column(name = "waiting")
    @NotNull
    private boolean waiting = false;

    public String getName() {
        return name;
    }

    public long getNbSessions() {
        return nbSessions;
    }

    public void setNbSessions(long nbSessions) {
        this.nbSessions = nbSessions;
    }

    public List<SourceStepAggregation> getSteps() {
        return steps;
    }

    public void setSteps(List<SourceStepAggregation> steps) {
        this.steps = steps;
    }

    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(OffsetDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }
}
