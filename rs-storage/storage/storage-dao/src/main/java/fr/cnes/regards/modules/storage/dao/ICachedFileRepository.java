package fr.cnes.regards.modules.storage.dao;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.storage.domain.database.CachedFile;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface ICachedFileRepository extends JpaRepository<CachedFile, Long>{

    Set<CachedFile> findAllByChecksumIn(Set<String> checksums);

    CachedFile findOneByChecksum(String checksum);
}
