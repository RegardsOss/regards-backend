package fr.cnes.regards.modules.crawler.domain;

/**
 * Ingestion status enumeration
 *
 * @author oroussel
 */
public enum IngestionStatus {
    /**
     * Datasource has just been created, no ingestion has been done yet
     */
    NEW(false, false),
    /**
     * Ingestion is in progress, some data may have been ingested but not all.
     */
    STARTED(false, false),
    /**
     * Last ingestion finished without error
     */
    FINISHED(true, true),
    /**
     * Last ingestion finished with warnings
     */
    FINISHED_WITH_WARNINGS(true, true),
    /**
     * Last ingestion finished with errors only for the first page of data.
     * It may indicate that the datasource is wrongly configured or that the datasource is not available
     */
    ERROR(true, false),
    /**
     * Last ingestion has been partially done : an error occurred while ingesting, but some data has been successfully ingested.
     */
    NOT_FINISHED(true, false),
    /**
     * Datasource plugin is inactive, no ingestion will be done until it is activated again.
     */
    INACTIVE(true, false);

    /**
     * A status is final if it cannot be changed without a human intervention.
     */
    private final boolean isFinal;

    private final boolean isSuccess;

    IngestionStatus(boolean isFinal, boolean isSuccess) {
        this.isFinal = isFinal;
        this.isSuccess = isSuccess;

    }

    public boolean isFinal() {
        return this.isFinal;
    }

    public boolean isSuccess() {
        return this.isSuccess;
    }

}
