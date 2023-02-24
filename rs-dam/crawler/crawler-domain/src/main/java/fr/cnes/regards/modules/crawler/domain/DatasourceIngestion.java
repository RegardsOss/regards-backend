package fr.cnes.regards.modules.crawler.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Datasource ingestion entity. Indicates the status of the last and current datsource ingestion.
 *
 * @author oroussel
 */
@Entity
@Table(name = "t_datasource_ingestion")
public class DatasourceIngestion {

    /**
     * Id is datasource id (see table pluginConf businnessId)
     */
    @Id
    @Column(name = "ds_id", length = 36)
    private String id;

    @Column(name = "label", length = 255)
    private String label;

    /**
     * Date of last ingestion (null if none yet)
     */
    @Column(name = "last_ingest_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastIngestDate;

    /**
     * Date of next planned ingest date (= last ingest date + refresh rate of datasource plugin)
     */
    @Column(name = "next_planned_ingest_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime nextPlannedIngestDate;

    /**
     * Duration from Status.STARTED to Status.FINISHED with second representation format (for example PT8H6M12.345S --
     * "8 hours 6 mn 12.345 s"
     */
    @Column(name = "duration", length = 20)
    private String duration = null;

    /**
     * Status of previous or current ingestion
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private IngestionStatus status = IngestionStatus.NEW;

    /**
     * Date of status change (default to object creation with NEW status if nothing provided)
     */
    @Column(name = "status_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime statusDate = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);

    /**
     * Last ingestion saved objects count
     */
    @Column(name = "saved_objects_count")
    private Integer savedObjectsCount;

    /**
     * Last ingestion number of objects that couldn't be saved
     */
    @Column(name = "error_objects_count")
    private Integer inErrorObjectsCount;

    @Embedded
    private CrawlingCursor cursor;

    /**
     * When status is ERROR, the exception stack trace
     */
    @Column
    @Type(type = "text")
    private String stackTrace;

    public DatasourceIngestion() {
    }

    public DatasourceIngestion(String id) {
        super();
        this.id = id;
    }

    public DatasourceIngestion(String id, OffsetDateTime nextPlannedIngestDate, String label) {
        this(id);
        this.nextPlannedIngestDate = nextPlannedIngestDate;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OffsetDateTime getLastIngestDate() {
        return lastIngestDate;
    }

    public void setLastIngestDate(OffsetDateTime lastIngestDate) {
        this.lastIngestDate = lastIngestDate;
    }

    public IngestionStatus getStatus() {
        return status;
    }

    public void setStatus(IngestionStatus status) {
        OffsetDateTime now = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);
        if ((status == IngestionStatus.FINISHED) && (statusDate != null)) {
            duration = Duration.between(statusDate, now).toString();
        } else {
            duration = null;
        }
        this.status = status;
        statusDate = now;
    }

    public OffsetDateTime getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(OffsetDateTime statusDate) {
        this.statusDate = statusDate;
    }

    public OffsetDateTime getNextPlannedIngestDate() {
        return nextPlannedIngestDate;
    }

    public void setNextPlannedIngestDate(OffsetDateTime nextPlannedIngestDate) {
        this.nextPlannedIngestDate = nextPlannedIngestDate;
    }

    public Integer getSavedObjectsCount() {
        return savedObjectsCount;
    }

    public void setSavedObjectsCount(Integer savedObjectsCount) {
        this.savedObjectsCount = savedObjectsCount;
    }

    public Integer getInErrorObjectsCount() {
        return inErrorObjectsCount;
    }

    public void setInErrorObjectsCount(Integer inErrorObjectsCount) {
        this.inErrorObjectsCount = inErrorObjectsCount;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public CrawlingCursor getCursor() {
        return cursor;
    }

    public void setCursor(CrawlingCursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DatasourceIngestion that = (DatasourceIngestion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DatasourceIngestion [id="
               + id
               + ", lastIngestDate="
               + lastIngestDate
               + ", status="
               + status
               + ", statusDate="
               + statusDate
               + "]";
    }

    /**
     * Set date history for next ingestion
     */
    public void setLastEntityDate(OffsetDateTime lastEntityDate, OffsetDateTime penultimateLastEntityDate) {
        // this last entity date becomes the previous last entity date of the next ingestion
        if (cursor == null) {
            cursor = new CrawlingCursor(lastEntityDate);
        } else {
            cursor.setLastEntityDate(lastEntityDate);
        }
        cursor.setPreviousLastEntityDate(penultimateLastEntityDate);
    }
}
