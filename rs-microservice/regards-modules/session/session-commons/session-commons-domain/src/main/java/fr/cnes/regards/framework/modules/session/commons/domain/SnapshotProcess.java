package fr.cnes.regards.framework.modules.session.commons.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

import java.util.Objects;
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
    @Column(name = "last_update_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdateDate;

    /**
     * If there is an ongoing update on {@link SessionStep}
     */
    @Column(name = "job_id")
    private UUID jobId;

    public SnapshotProcess(String source, OffsetDateTime lastUpdateDate, UUID jobId) {
        this.source = source;
        this.lastUpdateDate = lastUpdateDate;
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

    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(OffsetDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SnapshotProcess that = (SnapshotProcess) o;
        return source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source);
    }
}
