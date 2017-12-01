package fr.cnes.regards.modules.storage.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.database.MonitoringAggregation;

/**
 * DAO to access metadata of files associated to aips into the database
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IDataFileDao {

    /**
     * Find all data files which state is the given one and which are associated to the provided aip
     * @param stored
     * @param aip
     * @return data files which state is the given one and which are associated to the provided aip
     */
    Set<DataFile> findAllByStateAndAip(DataFileState stored, AIP aip);

    /**
     * Find all data files which state is the provided one and that are associated to at least one of the provided aips
     * @param dataFileState
     * @param aips
     * @return data files which state is the provided one and that are associated to at least one of the provided aips
     */
    Set<DataFile> findAllByStateAndAipIn(DataFileState dataFileState, Collection<AIP> aips);

    /**
     * Find all {@link DataFile}s associated to the given {@link AIP}
     * @param aip {@link AIP}
     * @return {@link DataFile}s
     */
    Set<DataFile> findAllByAip(AIP aip);

    /**
     * Save a data file into the database
     * @param prepareFailed
     * @return saved data file
     */
    DataFile save(DataFile prepareFailed);

    /**
     * Save data files into the database
     * @param dataFiles
     * @return saved data files
     */
    Collection<DataFile> save(Collection<DataFile> dataFiles);

    /**
     * Find the data file associated to the given aip and which type the provided one
     * @param aip
     * @param dataType
     * @return the data file wrapped into an optional to avoid nulls
     */
    Optional<DataFile> findByAipAndType(AIP aip, DataType dataType);

    /**
     * Remove all data files from the database
     */
    void deleteAll();

    /**
     * Retrieve a data file by its id
     * @param dataFileId
     * @return the data file wrapped into an optional to avoid nulls
     */
    Optional<DataFile> findOneById(Long dataFileId);

    /**
     * Find all data files which checksum is one of the provided ones
     * @param checksums
     * @return data files which checksum is one of the provided ones
     */
    Set<DataFile> findAllByChecksumIn(Set<String> checksums);

    /**
     * Remove a data file from the database
     * @param data
     */
    void remove(DataFile data);

    /**
     * Calculate the monitoring information on all data files of the database
     * @return the monitoring aggregation
     */
    Collection<MonitoringAggregation> getMonitoringAggregation();
}
