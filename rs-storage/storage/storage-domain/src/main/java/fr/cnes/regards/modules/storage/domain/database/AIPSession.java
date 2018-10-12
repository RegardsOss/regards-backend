package fr.cnes.regards.modules.storage.domain.database;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

/**
 * Entities to group {@link AIPSession}s. The {@link AIPSession#getLastActivationDate()}
 * is updated after each modification on a {@link AIPSession} of the session.
 *
 * @author Sébastien Binda
 * @author Léo Mieulet
 */
@Entity
@Table(name = "t_aip_session", indexes = { @Index(name = "idx_aip_session", columnList = "id") })
public class AIPSession {

    /**
     * Session identifier (name)
     */
    @Id
    private String id;

    @NotNull
    @Column(name = "last_activation_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastActivationDate;

    @Transient
    private long aipsCount = 0;

    @Transient
    private long deletedAipsCount = 0;

    @Transient
    private long errorAipsCount = 0;

    @Transient
    private long queuedAipsCount = 0;

    @Transient
    private long storedAipsCount = 0;

    @Transient
    private long storedDataFilesCount = 0;

    @Transient
    private long dataFilesCount = 0;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OffsetDateTime getLastActivationDate() {
        return lastActivationDate;
    }

    public void setLastActivationDate(OffsetDateTime lastActivationDate) {
        this.lastActivationDate = lastActivationDate;
    }

    public long getAipsCount() {
        return aipsCount;
    }

    public void setAipsCount(long aipsCount) {
        this.aipsCount = aipsCount;
    }

    public long getDeletedAipsCount() {
        return deletedAipsCount;
    }

    public void setDeletedAipsCount(long deletedAipsCount) {
        this.deletedAipsCount = deletedAipsCount;
    }

    public long getErrorAipsCount() {
        return errorAipsCount;
    }

    public void setErrorAipsCount(long errorAipsCount) {
        this.errorAipsCount = errorAipsCount;
    }

    public long getQueuedAipsCount() {
        return queuedAipsCount;
    }

    public void setQueuedAipsCount(long queuedAipsCount) {
        this.queuedAipsCount = queuedAipsCount;
    }

    public long getStoredAipsCount() {
        return storedAipsCount;
    }

    public void setStoredAipsCount(long storedAipsCount) {
        this.storedAipsCount = storedAipsCount;
    }

    public long getStoredDataFilesCount() {
        return storedDataFilesCount;
    }

    public void setStoredDataFilesCount(long storedDataFilesCount) {
        this.storedDataFilesCount = storedDataFilesCount;
    }

    public long getDataFilesCount() {
        return dataFilesCount;
    }

    public void setDataFilesCount(long dataFilesCount) {
        this.dataFilesCount = dataFilesCount;
    }

}
