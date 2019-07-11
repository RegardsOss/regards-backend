package fr.cnes.regards.modules.storagelight.domain.database;

/**
 * Aggregation of the monitoring information of the data file per data storage
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface StorageMonitoringAggregation {

    String getStorage();

    Long getUsedSize();

    Long getNumberOfFileReference();

    Long getLastFileReferenceId();
}
