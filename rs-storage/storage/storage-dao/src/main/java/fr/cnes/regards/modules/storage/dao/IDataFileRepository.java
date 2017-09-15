package fr.cnes.regards.modules.storage.dao;

import javax.persistence.Entity;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.storage.domain.database.AIPDataBase;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IDataFileRepository extends JpaRepository<DataFile, Long> {

    Set<DataFile> findAllByStateAndAipDataBase(DataFileState stored, AIPDataBase aipDataBase);

    @EntityGraph(value = "graph.datafile.full")
    DataFile findByAipDataBaseAndType(AIPDataBase aipDataBase, DataType dataType);

    @EntityGraph(value = "graph.datafile.full")
    DataFile findOneById(Long dataFileId);

    @EntityGraph(value = "graph.datafile.full")
    Set<DataFile> findAllByChecksumIn(Set<String> checksums);
}
