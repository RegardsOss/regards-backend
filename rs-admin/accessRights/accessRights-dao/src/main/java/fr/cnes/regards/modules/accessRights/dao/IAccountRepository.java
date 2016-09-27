/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.accessRights.domain.Account;

public interface IAccountRepository extends CrudRepository<Account, Long> {

}
