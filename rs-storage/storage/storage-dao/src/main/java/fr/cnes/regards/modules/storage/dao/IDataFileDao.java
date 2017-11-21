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
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IDataFileDao {

    Set<DataFile> findAllByStateAndAip(DataFileState stored, AIP aip);

    Set<DataFile> findAllByStateAndAipIn(DataFileState dataFileState, Collection<AIP> aips);

    /**
     * Find all {@link DataFile}s associated to the given {@link AIP}
     * @param aip {@link AIP}
     * @return {@link DataFile}s
     */
    Set<DataFile> findAllByAip(AIP aip);

    DataFile save(DataFile prepareFailed);

    Collection<DataFile> save(Collection<DataFile> dataFiles);

    Optional<DataFile> findByAipAndType(AIP aip, DataType dataType);

    void deleteAll();

    Optional<DataFile> findOneById(Long dataFileId);

    Set<DataFile> findAllByChecksumIn(Set<String> checksums);

    void remove(DataFile data);

    Collection<MonitoringAggregation> getMonitoringAggregation();
}
