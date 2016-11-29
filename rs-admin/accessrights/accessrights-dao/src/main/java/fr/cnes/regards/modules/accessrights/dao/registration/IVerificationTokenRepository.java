/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.registration;

import java.util.Date;
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
 */
@InstanceEntity
public interface IVerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String pToken);

    VerificationToken findByAccount(Account pAccount);

    Stream<VerificationToken> findAllByExpiryDateLessThan(Date pNow);

    void deleteByExpiryDateLessThan(Date pNow);

    @Modifying
    @Query("delete from T_VERIFICATION_TOKEN t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(Date pNow);
}