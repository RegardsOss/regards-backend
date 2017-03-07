/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.registration;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.registration.VerificationToken;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link VerificationToken}s.<br>
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@InstanceEntity
public interface IVerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String pToken);

    Optional<VerificationToken> findByAccount(Account pAccount);

    Stream<VerificationToken> findAllByExpiryDateLessThan(LocalDateTime pNow);

    void deleteByExpiryDateLessThan(LocalDateTime pNow);

    @Modifying
    @Query("delete from T_VERIFICATION_TOKEN t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDateTime pNow);
}