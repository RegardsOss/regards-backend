/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.instance;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.PasswordResetToken;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link PasswordResetToken}s.<br>
 *
 * @author Xavier-Alexandre Brochard
 */
@InstanceEntity
public interface IPasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String pToken);

    Optional<PasswordResetToken> findByAccount(Account pAccount);

    Stream<PasswordResetToken> findAllByExpiryDateLessThan(Date pNow);

    void deleteByExpiryDateLessThan(Date pNow);

    @Modifying
    @Query("delete from T_PASSWORD_RESET_TOKEN t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(Date pNow);
}