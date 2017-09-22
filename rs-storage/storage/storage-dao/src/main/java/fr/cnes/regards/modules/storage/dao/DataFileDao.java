package fr.cnes.regards.modules.storage.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.AIPDataBase;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;

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
        Optional<AIPDataBase> aipDatabase = getAipDataBase(aip);
        if (aipDatabase.isPresent()) {
            return repository.findAllByStateAndAipDataBase(stored, aipDatabase.get());
        } else {
            return Sets.newHashSet();
        }
    }

    @Override
    public DataFile save(DataFile prepareFailed) {
        Optional<AIPDataBase> aipDatabase = getAipDataBase(prepareFailed);
        if (aipDatabase.isPresent()) {
            prepareFailed.setAipDataBase(aipDatabase.get());
        }
        return repository.save(prepareFailed);
    }

    @Override
    public Optional<DataFile> findByAipAndType(AIP aip, DataType dataType) {
        Optional<AIPDataBase> aipDatabase = getAipDataBase(aip);
        if (aipDatabase.isPresent()) {
            return repository.findByAipDataBaseAndDataType(aipDatabase.get(), dataType);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Collection<DataFile> save(Collection<DataFile> dataFiles) {
        for (DataFile dataFile : dataFiles) {
            Optional<AIPDataBase> aipDatabase = getAipDataBase(dataFile);
            if (aipDatabase.isPresent()) {
                dataFile.setAipDataBase(aipDatabase.get());
            }
        }
        return repository.save(dataFiles);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public Optional<DataFile> findOneById(Long dataFileId) {
        return repository.findOneById(dataFileId);
    }

    @Override
    public Set<DataFile> findAllByChecksumIn(Set<String> checksums) {
        return repository.findAllByChecksumIn(checksums);
    }

    @Override
    public void remove(DataFile data) {
        Optional<AIPDataBase> aipDatabase = getAipDataBase(data);
        if (aipDatabase.isPresent()) {
            // TODO : Why ???
            data.setAipDataBase(aipDatabase.get());
        }
        repository.delete(data);
    }

    private Optional<AIPDataBase> getAipDataBase(AIP aip) {
        return aipRepo.findOneByIpId(aip.getIpId());
    }

    public Optional<AIPDataBase> getAipDataBase(DataFile dataFile) {
        return aipRepo.findOneByIpId(dataFile.getAipDataBase().getIpId());
    }
}
