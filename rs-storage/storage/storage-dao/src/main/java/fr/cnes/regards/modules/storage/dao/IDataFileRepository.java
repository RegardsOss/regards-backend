package fr.cnes.regards.modules.storage.dao;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.storage.domain.database.AIPDataBase;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IDataFileRepository extends JpaRepository<DataFile, Long> {

    Set<DataFile> findAllByStateAndAipDataBase(DataFileState stored, AIPDataBase aipDataBase);
}
