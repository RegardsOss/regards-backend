package fr.cnes.regards.modules.storage.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IDataFileRepository extends JpaRepository<DataFile, Long> {

    @EntityGraph(value = "graph.datafile.full")
    Set<DataFile> findAllByStateAndAipEntity(DataFileState stored, AIPEntity aipEntity);

    @EntityGraph(value = "graph.datafile.full")
    Set<DataFile> findAllByAipEntity(AIPEntity aipEntity);

    @EntityGraph(value = "graph.datafile.full")
    Optional<DataFile> findByAipEntityAndDataType(AIPEntity aipEntity, DataType dataType);

    @EntityGraph(value = "graph.datafile.full")
    Optional<DataFile> findOneById(Long dataFileId);

    @EntityGraph(value = "graph.datafile.full")
    Set<DataFile> findAllByChecksumIn(Set<String> checksums);

    @EntityGraph(value = "graph.datafile.full")
    Set<DataFile> findAllByStateAndAipEntityIn(DataFileState dataFileState, Collection<AIPEntity> aipDataBases);
}
