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

import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link EmailVerificationToken}s.<br>
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
public interface IVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * Find token with given string
     * @param pToken the string
     * @return the optional token
     */
    Optional<EmailVerificationToken> findByToken(String pToken);

    /**
     * Find token with given ProjectUser
     * @param pProjectUser the project user
     * @return the option token
     */
    Optional<EmailVerificationToken> findByProjectUser(ProjectUser pProjectUser);

    /**
     * Find all token with expiry date less than given date
     * @param pNow the given date
     * @return a stream of matching tokens
     */
    Stream<EmailVerificationToken> findAllByExpiryDateLessThan(LocalDateTime pNow);

    /**
     * Delete all tokens with expiry date less than given date
     * @param pNow the fien date
     */
    void deleteByExpiryDateLessThan(LocalDateTime pNow);

    /**
     * Delete all token which expired since given date
     * @param pNow the given date
     */
    @Modifying
    @Query("delete from EmailVerificationToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDateTime pNow);
}