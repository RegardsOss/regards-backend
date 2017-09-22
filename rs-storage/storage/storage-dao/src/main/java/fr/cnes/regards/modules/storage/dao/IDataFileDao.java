package fr.cnes.regards.modules.storage.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IDataFileDao {

    Set<DataFile> findAllByStateAndAip(DataFileState stored, AIP aip);

    DataFile save(DataFile prepareFailed);

    Optional<DataFile> findByAipAndType(AIP aip, DataType dataType);

    Collection<DataFile> save(Collection<DataFile> dataFiles);

    void deleteAll();

    Optional<DataFile> findOneById(Long dataFileId);

    Set<DataFile> findAllByChecksumIn(Set<String> checksums);

    void remove(DataFile data);
}
