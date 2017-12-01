package fr.cnes.regards.modules.storage.domain.database;

/**
 * Aggregation of the monitoring information of the data file per data storage
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface MonitoringAggregation {

    /**
     * @return the data storage used id
     */
    Long getDataStorageUsedId();

    /**
     * @return the used size
     */
    Long getUsedSize();
}
