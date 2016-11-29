/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.instance;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.VerificationToken;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link VerificationToken}s.<br>
 *
 * @author Xavier-Alexandre Brochard
 */
@InstanceEntity
public interface IVerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    VerificationToken findByToken(String pToken);

    VerificationToken findByAccount(Account pAccount);
}