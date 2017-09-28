package fr.cnes.regards.modules.storage.dao;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.CachedFileState;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface ICachedFileRepository extends JpaRepository<CachedFile, Long> {

    Set<CachedFile> findAllByChecksumIn(Set<String> checksums);

    Optional<CachedFile> findOneByChecksum(String checksum);

    CachedFile removeByChecksum(String checksum);

    Set<CachedFile> findByState(CachedFileState pQueued);
}
