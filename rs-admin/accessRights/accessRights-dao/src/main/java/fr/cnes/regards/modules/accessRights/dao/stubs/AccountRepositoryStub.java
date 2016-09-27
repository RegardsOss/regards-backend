/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.stubs;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.dao.IAccountRepository;
import fr.cnes.regards.modules.accessRights.domain.Account;

@Repository
@Profile("test")
@Primary
public class AccountRepositoryStub extends RepositoryStub<Account> implements IAccountRepository {

}
