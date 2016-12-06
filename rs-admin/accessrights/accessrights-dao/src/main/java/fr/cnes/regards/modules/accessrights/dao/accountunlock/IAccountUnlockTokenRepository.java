/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.accountunlock;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.domain.accountunlock.AccountUnlockToken;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link AccountUnlockToken}s.<br>
 *
 * @author Xavier-Alexandre Brochard
 */
@InstanceEntity
public interface IAccountUnlockTokenRepository extends JpaRepository<AccountUnlockToken, Long> {

    Optional<AccountUnlockToken> findByToken(String pToken);

    Optional<AccountUnlockToken> findByAccount(Account pAccount);

    Stream<AccountUnlockToken> findAllByExpiryDateLessThan(Date pNow);

    void deleteByExpiryDateLessThan(Date pNow);

    @Modifying
    @Query("delete from T_VERIFICATION_TOKEN t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(Date pNow);
}