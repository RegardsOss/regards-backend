package fr.cnes.regards.modules.storagelight.domain;

/**
 * Aggregation of the monitoring information of the data file per data storage
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface StorageMonitoringAggregation {

    String getDataStorage();

    Long getUsedSize();

    Long getNumberOfFileReference();

    Long getLastFileReferenceId();
}
