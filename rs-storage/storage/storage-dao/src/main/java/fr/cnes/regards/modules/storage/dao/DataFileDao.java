package fr.cnes.regards.modules.storage.dao;

import java.util.Collection;
import java.util.Set;

import fr.cnes.regards.framework.urn.DataType;
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

    @Override
    public DataFile save(DataFile prepareFailed) {
        return repository.save(prepareFailed);
    }

    @Override
    public DataFile findByAipAndType(AIP aip, DataType dataType) {
        return repository.findByAipDataBaseAndType(new AIPDataBase(aip), dataType);
    }

    @Override
    public Collection<DataFile> save(Collection<DataFile> dataFiles) {
        return repository.save(dataFiles);
    }
}
