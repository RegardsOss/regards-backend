package fr.cnes.regards.modules.crawler.domain;

import java.time.OffsetDateTime;

/**
 * Ingestion result summary
 *
 * @author oroussel
 */
public class IngestionResult {

    private final OffsetDateTime lastEntityDate;

    private OffsetDateTime date;

    private final int savedObjectsCount;

    private final int inErrorObjectsCount;

    public IngestionResult(OffsetDateTime date,
                           int saveObjectsCount,
                           int inErrorObjectsCount,
                           OffsetDateTime lastEntityDate) {
        super();
        this.date = date;
        this.savedObjectsCount = saveObjectsCount;
        this.inErrorObjectsCount = inErrorObjectsCount;
        this.lastEntityDate = lastEntityDate;
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
}
