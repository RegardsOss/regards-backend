/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.accountunlock;

import java.time.LocalDateTime;
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
 * @author Christophe Mertz
 */
@InstanceEntity
public interface IAccountUnlockTokenRepository extends JpaRepository<AccountUnlockToken, Long> {

    Optional<AccountUnlockToken> findByToken(String pToken);

    Optional<AccountUnlockToken> findByAccount(Account pAccount);

    Stream<AccountUnlockToken> findAllByExpiryDateLessThan(LocalDateTime pNow);

    void deleteByExpiryDateLessThan(LocalDateTime pNow);

    @Modifying
    @Query("delete from AccountUnlockToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDateTime pNow);

    /**
     * Delete all {@link AccountUnlockToken}s for the passed {@link Account}
     *
     * @param pAccount
     *            the account
     * @param pToken
     *            the token
     */
    void deleteAllByAccount(Account pAccount);
}