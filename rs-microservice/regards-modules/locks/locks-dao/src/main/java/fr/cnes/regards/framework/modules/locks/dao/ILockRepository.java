package fr.cnes.regards.framework.modules.locks.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.modules.locks.domain.Lock;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Repository
public interface ILockRepository extends JpaRepository<Lock, Long> {

    Optional<Lock> findByLockingClassNameAndLockName(String lockingClassName, String lockName);
}
