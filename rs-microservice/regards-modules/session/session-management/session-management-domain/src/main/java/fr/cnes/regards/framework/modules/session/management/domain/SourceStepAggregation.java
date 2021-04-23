package fr.cnes.regards.framework.modules.session.management.domain;

import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
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
     * Name of the source aggregated
     */
    @Id
    @Column(name = "source")
    private String source;

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
    @Column(name="total_id")
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
    private StepState state;

}
