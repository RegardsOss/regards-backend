package fr.cnes.regards.modules.storage.domain.database;

/**
 * Aggregation of the monitoring information of {@link FileReference} per storage location
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface StorageMonitoringAggregation {

    /**
     * Aggregated storage location name
     */
    String getStorage();

    /**
     * Total size of {@link FileReference} in the storage location
     */
    Long getUsedSize();

    /**
     * Number of {@link FileReference} in the storage location
     */
    Long getNumberOfFileReference();

    /**
     * Last {@link FileReference} identifier of the storage location aggregated here.<br/>
     * This information is useful to optimize aggregation calculation when updating information from a previous aggregation.
     */
    Long getLastFileReferenceId();
}
