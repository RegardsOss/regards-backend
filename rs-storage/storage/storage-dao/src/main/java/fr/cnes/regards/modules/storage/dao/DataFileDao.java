package fr.cnes.regards.modules.storage.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.database.MonitoringAggregation;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class DataFileDao implements IDataFileDao {

    /**
     * {@link IStorageDataFileRepository} instance
     */
    @Autowired
    private final IStorageDataFileRepository repository;

    /**
     * {@link IAIPEntityRepository} instance
     */
    @Autowired
    private final IAIPEntityRepository aipRepo;

    /**
     * Constructor setting the parameters as attributes
     * @param repository
     * @param aipRepo
     */
    public DataFileDao(IStorageDataFileRepository repository, IAIPEntityRepository aipRepo) {
        this.repository = repository;
        this.aipRepo = aipRepo;
    }

    @Override
    public Set<StorageDataFile> findAllByStateAndAip(DataFileState stored, AIP aip) {
        Optional<AIPEntity> aipDatabase = getAipDataBase(aip);
        if (aipDatabase.isPresent()) {
            return repository.findAllByStateAndAipEntity(stored, aipDatabase.get());
        } else {
            return Sets.newHashSet();
        }
    }

    @Override
    public long findAllByStateAndAipSession(DataFileState stored, String session) {
        return repository.countByStateAndAipEntitySessionId(stored, session);
    }

    @Override
    public Page<StorageDataFile> findAllByState(DataFileState state, Pageable page) {
        return repository.findAllByState(state, page);
    }

    @Override
    public Page<StorageDataFile> findPageByState(DataFileState state, Pageable pageable) {
        // first lets get the storageDataFile without any join(no graph)
        Page<Long> ids = repository.findIdPageByState(state, pageable);
        Set<StorageDataFile> pageContent = repository.findAllDistinctByIdIn(ids.getContent());
        return new PageImpl<>(pageContent.stream().collect(Collectors.toList()),
                PageRequest.of(ids.getNumber(), ids.getSize()), ids.getTotalElements());
    }

    @Override
    public Set<StorageDataFile> findAllByStateAndAipIn(DataFileState dataFileState, Collection<AIP> aips) {
        Set<AIPEntity> aipDataBases = Sets.newHashSet();
        for (AIP aip : aips) {
            Optional<AIPEntity> aipDatabase = getAipDataBase(aip);
            if (aipDatabase.isPresent()) {
                aipDataBases.add(aipDatabase.get());
            }
        }
        if (aipDataBases.isEmpty()) {
            return Sets.newHashSet();
        } else {
            return repository.findAllByStateAndAipEntityIn(dataFileState, aipDataBases);
        }
    }

    @Override
    public Set<StorageDataFile> findAllByAip(AIP aip) {
        Optional<AIPEntity> aipDatabase = getAipDataBase(aip);
        if (aipDatabase.isPresent()) {
            return repository.findAllByAipEntity(aipDatabase.get());
        } else {
            return Sets.newHashSet();
        }
    }

    @Override
    public Set<StorageDataFile> findAllByAipIn(Collection<AIP> aips) {
        Collection<AIPEntity> aipEntities = findAipsDataBase(aips);
        return repository.findAllByAipEntityIn(aipEntities);
    }

    @Override
    public StorageDataFile save(StorageDataFile dataFile) {
        Optional<AIPEntity> aipDatabase = getAipDataBase(dataFile);
        if (aipDatabase.isPresent()) {
            dataFile.setAipEntity(aipDatabase.get());
        }
        return repository.save(dataFile);
    }

    @Override
    public Collection<StorageDataFile> save(Collection<StorageDataFile> dataFiles) {
        for (StorageDataFile dataFile : dataFiles) {
            Optional<AIPEntity> aipDatabase = getAipDataBase(dataFile);
            if (aipDatabase.isPresent()) {
                dataFile.setAipEntity(aipDatabase.get());
            }
        }
        return repository.saveAll(dataFiles);
    }

    @Override
    public Set<StorageDataFile> findByAipAndType(AIP aip, DataType dataType) {
        Optional<AIPEntity> aipDatabase = getAipDataBase(aip);
        if (aipDatabase.isPresent()) {
            return repository.findByAipEntityAndDataType(aipDatabase.get(), dataType);
        } else {
            return Sets.newHashSet();
        }
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public Optional<StorageDataFile> findOneById(Long dataFileId) {
        return repository.findOneById(dataFileId);
    }

    @Override
    public Optional<StorageDataFile> findLockedOneById(Long dataFileId) {
        return repository.findLockedOneById(dataFileId);
    }

    @Override
    public Set<StorageDataFile> findAllByChecksumIn(Set<String> checksums) {
        return repository.findAllByChecksumIn(checksums);
    }

    @Override
    public Page<StorageDataFile> findPageByChecksumIn(Set<String> checksums, Pageable pageable) {
        // first lets get the storageDataFile without any join(no graph)
        Page<Long> ids = repository.findIdPageByChecksumIn(checksums, pageable);
        List<StorageDataFile> pageContent = repository.findAllDistinctByIdIn(ids.getContent()).stream()
                .collect(Collectors.toList());
        return new PageImpl<>(pageContent, PageRequest.of(ids.getNumber(), ids.getSize()), ids.getTotalElements());
    }

    @Override
    public Page<StorageDataFile> findPageByStateAndChecksumIn(DataFileState dataFileState, Set<String> checksums,
            Pageable pageable) {
        // first lets get the storageDataFile without any join(no graph)
        Page<Long> ids = repository.findIdPageByStateAndChecksumIn(dataFileState, checksums, pageable);
        List<StorageDataFile> pageContent = repository.findAllDistinctByIdIn(ids.getContent()).stream()
                .collect(Collectors.toList());
        return new PageImpl<>(pageContent, PageRequest.of(ids.getNumber(), ids.getSize()), ids.getTotalElements());
    }

    @Override
    public void remove(StorageDataFile data) {
        repository.deleteById(data.getId());
    }

    @Override
    public Collection<MonitoringAggregation> getMonitoringAggregation() {
        return repository.getMonitoringAggregation();
    }

    @Override
    public long countByChecksumAndStorageDirectory(String checksum, String storageDirectory) {
        if (storageDirectory != null) {
            return repository.countByChecksumAndStorageDirectory(checksum, storageDirectory);
        }
        return repository.countByChecksum(checksum);
    }

    @Override
    public long countByAip(AIP aip) {
        Optional<AIPEntity> fromDbOpt = getAipDataBase(aip);
        if (fromDbOpt.isPresent()) {
            return repository.countByAipEntity(fromDbOpt.get());
        } else {
            return 0;
        }
    }

    private Optional<AIPEntity> getAipDataBase(AIP aip) {
        return aipRepo.findOneByAipId(aip.getId().toString());
    }

    private Collection<AIPEntity> findAipsDataBase(Collection<AIP> aips) {
        return aipRepo.findAllByAipIdIn(aips.stream().map(aip -> aip.getId().toString()).collect(Collectors.toSet()));
    }

    private Optional<AIPEntity> getAipDataBase(StorageDataFile dataFile) {
        return aipRepo.findOneByAipId(dataFile.getAipEntity().getAipId());
    }

    @Override
    public long countByAipAndStateNotIn(AIP aip, Collection<DataFileState> dataFilesStates) {
        Optional<AIPEntity> fromDbOpt = getAipDataBase(aip);
        if (fromDbOpt.isPresent()) {
            return repository.countByAipEntityAndStateNotIn(fromDbOpt.get(), dataFilesStates);
        } else {
            return 0;
        }
    }

    @Override
    public long countByAipAndByState(AIP aip, DataFileState dataFileState) {
        Optional<AIPEntity> fromDbOpt = getAipDataBase(aip);
        if (fromDbOpt.isPresent()) {
            return repository.countByAipEntityAndState(fromDbOpt.get(), dataFileState);
        } else {
            return 0;
        }
    }

    @Override
    public long findAllByAipSession(String id) {
        return repository.countByAipEntitySessionId(id);
    }

    @Override
    public Set<StorageDataFile> findAllByAipIpIdIn(Collection<String> ipIds) {
        return repository.findAllByAipEntityAipIdIn(ipIds);
    }

    @Override
    public List<StorageDataFile> findAllByAipInQuery(String aipQuery) {
        return repository.findAllByAipInQuery(aipQuery);
    }
}
