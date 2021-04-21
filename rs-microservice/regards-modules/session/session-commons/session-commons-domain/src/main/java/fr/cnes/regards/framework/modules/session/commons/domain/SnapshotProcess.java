package fr.cnes.regards.framework.modules.session.commons.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.validation.constraints.NotNull;

/**
 * Process used to create or update {@link SessionStep}s from StepPropertyUpdateEventRequests
 *
 * @author Iliana Ghazali
 **/
@Entity
@Table(name = "t_snapshot_process")
public class SnapshotProcess {

    /**
     * Name of the source
     */
    @Id
    @Column(name = "source")
    @NotNull
    private String source;

    /**
     * Last date since when the SessionSteps were updated with StepPropertyUpdateEventRequests
     */
    @Column(name = "last_update")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdate;

    /**
     * If there is an ongoing update on {@link SessionStep}
     */
    @Column(name = "job_id")
    private UUID jobId;

    public SnapshotProcess(String source, OffsetDateTime lastUpdate, UUID jobId) {
        this.source = source;
        this.lastUpdate = lastUpdate;
        this.jobId = jobId;
    }

    public SnapshotProcess() {
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }
}
