/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.instance;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

@InstanceEntity
public interface IAccountRepository extends CrudRepository<Account, Long> {

    Account findOneByEmail(String pEmail);

}
