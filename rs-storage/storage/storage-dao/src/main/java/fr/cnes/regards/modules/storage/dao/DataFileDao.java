package fr.cnes.regards.modules.storage.dao;

import java.util.Set;

import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.AIPDataBase;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class DataFileDao implements IDataFileDao {

    private IDataFileRepository repository;

    @Override
    public Set<DataFile> findAllByStateAndAip(DataFileState stored, AIP aip) {
        return repository.findAllByStateAndAipDataBase(stored, new AIPDataBase(aip));
    }
}
