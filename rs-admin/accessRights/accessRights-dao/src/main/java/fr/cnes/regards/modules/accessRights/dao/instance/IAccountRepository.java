/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.instance;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.domain.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessRights.domain.instance.Account;

@InstanceEntity
public interface IAccountRepository extends CrudRepository<Account, Long> {

}
