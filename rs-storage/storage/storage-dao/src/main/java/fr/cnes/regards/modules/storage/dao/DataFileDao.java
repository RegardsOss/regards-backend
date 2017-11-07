package fr.cnes.regards.modules.storage.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
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
        Optional<AIPEntity> aipDatabase = getAipDataBase(aip);
        if (aipDatabase.isPresent()) {
            return repository.findAllByStateAndAipEntity(stored, aipDatabase.get());
        } else {
            return Sets.newHashSet();
        }
    }

    @Override
    public Set<DataFile> findAllByStateAndAipIn(DataFileState dataFileState, Collection<AIP> aips) {
        Set<AIPEntity> aipDataBases = Sets.newHashSet();
        for(AIP aip: aips) {
            Optional<AIPEntity> aipDatabase = getAipDataBase(aip);
            if (aipDatabase.isPresent()) {
                aipDataBases.add(aipDatabase.get());
            }
        }
        if(aipDataBases.isEmpty()) {
            return Sets.newHashSet();
        } else {
            return repository.findAllByStateAndAipEntityIn(dataFileState, aipDataBases);
        }
    }

    @Override
    public Set<DataFile> findAllByAip(AIP aip) {
        Optional<AIPEntity> aipDatabase = getAipDataBase(aip);
        if (aipDatabase.isPresent()) {
            return repository.findAllByAipEntity(aipDatabase.get());
        } else {
            return Sets.newHashSet();
        }
    }

    @Override
    public DataFile save(DataFile prepareFailed) {
        Optional<AIPEntity> aipDatabase = getAipDataBase(prepareFailed);
        if (aipDatabase.isPresent()) {
            prepareFailed.setAipEntity(aipDatabase.get());
        }
        return repository.save(prepareFailed);
    }

    @Override
    public Optional<DataFile> findByAipAndType(AIP aip, DataType dataType) {
        Optional<AIPEntity> aipDatabase = getAipDataBase(aip);
        if (aipDatabase.isPresent()) {
            return repository.findByAipEntityAndDataType(aipDatabase.get(), dataType);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Collection<DataFile> save(Collection<DataFile> dataFiles) {
        for (DataFile dataFile : dataFiles) {
            Optional<AIPEntity> aipDatabase = getAipDataBase(dataFile);
            if (aipDatabase.isPresent()) {
                dataFile.setAipEntity(aipDatabase.get());
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
        repository.delete(data.getId());
    }

    private Optional<AIPEntity> getAipDataBase(AIP aip) {
        return aipRepo.findOneByIpId(aip.getId().toString());
    }

    public Optional<AIPEntity> getAipDataBase(DataFile dataFile) {
        return aipRepo.findOneByIpId(dataFile.getAipEntity().getIpId());
    }
}
