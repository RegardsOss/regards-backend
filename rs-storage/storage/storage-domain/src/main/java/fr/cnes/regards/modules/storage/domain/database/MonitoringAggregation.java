package fr.cnes.regards.modules.storage.domain.database;

/**
 * Aggregation of the monitoring information of the data file per data storage
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface MonitoringAggregation {

    Long getDataStorageUsedId();

    Long getUsedSize();
}
