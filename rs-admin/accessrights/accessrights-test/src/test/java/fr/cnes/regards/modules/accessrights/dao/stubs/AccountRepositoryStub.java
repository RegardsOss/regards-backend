/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.stubs;

import java.util.stream.Stream;

import fr.cnes.regards.framework.test.repository.JpaRepositoryStub;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

//@Repository
//@Profile("test")
//@Primary
public class AccountRepositoryStub extends JpaRepositoryStub<Account> implements IAccountRepository {

    /**
     * An email
     */
    private static final String EMAIL_0 = "instance_admin@cnes.fr";

    /**
     * An other email
     */
    private static final String EMAIL_1 = "toto@toto.toto";

    /**
     * A first name
     */
    private static final String FIRSTNAME_0 = "Firstname";

    /**
     * An other first name
     */
    private static final String FIRSTNAME_1 = "Otherfirstname";

    /**
     * A first name
     */
    private static final String LASTNAME_0 = "Lastname";

    /**
     * An other first name
     */
    private static final String LASTNAME_1 = "Otherlastname";

    /**
     * A password
     */
    private static final String MDP_0 = "password";

    /**
     * An other password
     */
    private static final String MDP_1 = "otherpassword";

    public AccountRepositoryStub() {
        final Account account0 = new Account(EMAIL_0, FIRSTNAME_0, LASTNAME_0, MDP_0);
        account0.setStatus(AccountStatus.ACCEPTED);
        account0.setId(0L);

        final Account account1 = new Account(EMAIL_1, FIRSTNAME_1, LASTNAME_1, MDP_1);
        account0.setId(1L);

        entities.add(account0);
        entities.add(account1);
    }

    @Override
    public Account findOneByEmail(final String pEmail) {
        try (Stream<Account> stream = entities.stream()) {
            return stream.filter(e -> e.getEmail() == pEmail).findFirst().orElse(null);
        }
    }
}
