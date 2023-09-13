package fr.cnes.regards.modules.crawler.domain;

import java.time.OffsetDateTime;

/**
 * Ingestion result summary
 *
 * @author oroussel
 */
public class IngestionResult {

    private final OffsetDateTime lastEntityDate;

    /**
     * Propagate penultimate last entity date from last {@link fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor}
     */
    private final OffsetDateTime penultimateLastEntityDate;

    private OffsetDateTime date;

    private final Long lastId;

    private final Long previousLastId;

    private final int savedObjectsCount;

    private final int inErrorObjectsCount;

    public IngestionResult(OffsetDateTime date,
                           int saveObjectsCount,
                           int inErrorObjectsCount,
                           OffsetDateTime lastEntityDate,
                           OffsetDateTime penultimateLastEntityDate,
                           Long lastId,
                           Long previousLastId) {
        super();
        this.date = date;
        this.savedObjectsCount = saveObjectsCount;
        this.inErrorObjectsCount = inErrorObjectsCount;
        this.lastEntityDate = lastEntityDate;
        this.penultimateLastEntityDate = penultimateLastEntityDate;
        this.lastId = lastId;
        this.previousLastId = previousLastId;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime pDate) {
        date = pDate;
    }

    public int getSavedObjectsCount() {
        return savedObjectsCount;
    }

    public int getInErrorObjectsCount() {
        return inErrorObjectsCount;
    }

    public OffsetDateTime getLastEntityDate() {
        return lastEntityDate;
    }

    public OffsetDateTime getPenultimateLastEntityDate() {
        return penultimateLastEntityDate;
    }

    public Long getLastId() {
        return lastId;
    }

    public Long getPreviousLastId() {
        return previousLastId;
    }
}
