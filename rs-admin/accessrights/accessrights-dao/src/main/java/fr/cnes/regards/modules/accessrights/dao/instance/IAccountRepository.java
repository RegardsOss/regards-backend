/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.instance;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Interface for a JPA auto-generated CRUD repository managing {@link Account}s.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sylvain Vissiere-Guerinet
 */
@InstanceEntity
public interface IAccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find the single {@link Account} with passed <code>email</code>.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param pEmail
     *            The {@link Account}'s <code>email</code>
     * @return An optional account
     */
    Optional<Account> findOneByEmail(String pEmail);

    /**
     * Find all Account which status is not the one provided.
     * @param pStatus the status we do not want
     * @return the set of matching accounts
     */
    Set<Account> findAllByStatusNot(AccountStatus pStatus);

    /**
     * Find all Account which status is the one provided.
     * @param pStatus the status we want
     * @param pPageable the pageable object used by Spring for building the page of result
     * @return the page of matching accounts
     */
    Page<Account> findAllByStatus(AccountStatus pStatus, Pageable pPageable);

}
