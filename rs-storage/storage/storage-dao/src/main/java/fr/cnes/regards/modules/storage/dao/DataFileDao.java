package fr.cnes.regards.modules.storage.dao;

import java.util.Collection;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.AIPDataBase;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class DataFileDao implements IDataFileDao {

    @Autowired
    private IDataFileRepository repository;

    @Autowired
    private IAIPDataBaseRepository aipRepo;

    @Override
    public Set<DataFile> findAllByStateAndAip(DataFileState stored, AIP aip) {
        return repository.findAllByStateAndAipDataBase(stored, getAipDataBase(aip));
    }

    @Override
    public DataFile save(DataFile prepareFailed) {
        prepareFailed.setAipDataBase(getAipDataBase(prepareFailed));
        return repository.save(prepareFailed);
    }

    @Override
    public DataFile findByAipAndType(AIP aip, DataType dataType) {
        return repository.findByAipDataBaseAndDataType(getAipDataBase(aip), dataType);
    }

    @Override
    public Collection<DataFile> save(Collection<DataFile> dataFiles) {
        for (DataFile dataFile : dataFiles) {
            dataFile.setAipDataBase(getAipDataBase(dataFile));
        }
        return repository.save(dataFiles);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public DataFile findOneById(Long dataFileId) {
        return repository.findOneById(dataFileId);
    }

    @Override
    public Set<DataFile> findAllByChecksumIn(Set<String> checksums) {
        return repository.findAllByChecksumIn(checksums);
    }

    @Override
    public void remove(DataFile data) {
        data.setAipDataBase(getAipDataBase(data));
        repository.delete(data);
    }

    private AIPDataBase getAipDataBase(AIP aip) {
        return aipRepo.findOneByIpId(aip.getIpId());
    }

    public AIPDataBase getAipDataBase(DataFile dataFile) {
        return aipRepo.findOneByIpId(dataFile.getAipDataBase().getIpId());
    }
}
