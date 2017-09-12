package fr.cnes.regards.modules.storage.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.storage.domain.parameter.StorageParameter;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Repository
public interface IStorageParameterRepository extends JpaRepository<StorageParameter, Long> {

    StorageParameter findOneByName(String name);
}
