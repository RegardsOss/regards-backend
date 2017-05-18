/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.instance;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.passwordreset.PasswordResetToken;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link PasswordResetToken}s.<br>
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@InstanceEntity
public interface IPasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String pToken);

    Optional<PasswordResetToken> findByAccount(Account pAccount);

    Stream<PasswordResetToken> findAllByExpiryDateLessThan(LocalDateTime pNow);

    void deleteByExpiryDateLessThan(LocalDateTime pNow);

    @Modifying
    @Query("delete from PasswordResetToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDateTime pNow);
}