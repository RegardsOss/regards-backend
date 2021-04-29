package fr.cnes.regards.framework.modules.session.management.domain;

import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
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
    @SequenceGenerator(name = "aggSequence", initialValue = 1, sequenceName = "seq_agg")
    @GeneratedValue(generator = "aggSequence", strategy = GenerationType.SEQUENCE)
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
    @Column(name="total_in")
    @NotNull
    private long totalIn = 0L;

    /**
     * Number of events related to outputs
     */
    @Column(name="total_out")
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

    public SourceStepAggregation(){
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
}
