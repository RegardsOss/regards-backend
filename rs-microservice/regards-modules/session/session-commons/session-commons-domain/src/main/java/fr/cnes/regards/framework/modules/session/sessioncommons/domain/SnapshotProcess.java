package fr.cnes.regards.framework.modules.session.sessioncommons.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author Iliana Ghazali
 **/
@Entity
@Table(name = "t_snapshot_process")
public class SnapshotProcess {

    @Id
    @Column(name = "source")
    private String source;

    @Column(name = "last_update")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdate;

    @Column(name = "job_id")
    private UUID job_id;


    public SnapshotProcess(String source, OffsetDateTime lastUpdate, UUID job_id) {
        this.source = source;
        this.lastUpdate = lastUpdate;
        this.job_id = job_id;
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

    public UUID getJob_id() {
        return job_id;
    }

    public void setJob_id(UUID job_id) {
        this.job_id = job_id;
    }
}
