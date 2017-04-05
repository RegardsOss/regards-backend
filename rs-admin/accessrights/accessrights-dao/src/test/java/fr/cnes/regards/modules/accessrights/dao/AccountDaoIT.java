/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration(exclude = AmqpAutoConfiguration.class)
@InstanceTransactional
@TestPropertySource(locations = "classpath:tests.properties")
public class AccountDaoIT {

    private Account accAccepted;

    private Account accActive;

    private Account accInative;

    private Account accLocked;

    private Account accPending;

    @Autowired
    private IAccountRepository accRepo;

    @Before
    public void init() {
        accAccepted = new Account("accepted@toto.toto", "pFirstName", "pLastName", "pPassword");
        accAccepted = accRepo.save(accAccepted);
        accActive = new Account("active@toto.toto", "pFirstName", "pLastName", "pPassword");
        accActive = accRepo.save(accActive);
        accInative = new Account("inactive@toto.toto", "pFirstName", "pLastName", "pPassword");
        accInative = accRepo.save(accInative);
        accLocked = new Account("locked@toto.toto", "pFirstName", "pLastName", "pPassword");
        accLocked = accRepo.save(accLocked);
        accPending = new Account("pending@toto.toto", "pFirstName", "pLastName", "pPassword");
        accPending = accRepo.save(accPending);
    }

    @Test
    public void test() {
        Set<Account> notAccepted = accRepo.findAllByStatusNot(AccountStatus.ACCEPTED);
        Assert.assertFalse(notAccepted.contains(accAccepted));
        Assert.assertTrue(notAccepted.contains(accActive));
        Assert.assertTrue(notAccepted.contains(accInative));
        Assert.assertTrue(notAccepted.contains(accLocked));
        Assert.assertTrue(notAccepted.contains(accPending));
        Set<Account> notActive = accRepo.findAllByStatusNot(AccountStatus.ACTIVE);
        Assert.assertTrue(notActive.contains(accAccepted));
        Assert.assertFalse(notActive.contains(accActive));
        Assert.assertTrue(notActive.contains(accInative));
        Assert.assertTrue(notActive.contains(accLocked));
        Assert.assertTrue(notActive.contains(accPending));
        Set<Account> notInactive = accRepo.findAllByStatusNot(AccountStatus.INACTIVE);
        Assert.assertTrue(notInactive.contains(accAccepted));
        Assert.assertTrue(notInactive.contains(accActive));
        Assert.assertFalse(notInactive.contains(accInative));
        Assert.assertTrue(notInactive.contains(accLocked));
        Assert.assertTrue(notInactive.contains(accPending));
        Set<Account> notLocked = accRepo.findAllByStatusNot(AccountStatus.LOCKED);
        Assert.assertTrue(notLocked.contains(accAccepted));
        Assert.assertTrue(notLocked.contains(accActive));
        Assert.assertTrue(notLocked.contains(accInative));
        Assert.assertFalse(notLocked.contains(accLocked));
        Assert.assertTrue(notLocked.contains(accPending));
        Set<Account> notPending = accRepo.findAllByStatusNot(AccountStatus.PENDING);
        Assert.assertTrue(notPending.contains(accAccepted));
        Assert.assertTrue(notPending.contains(accActive));
        Assert.assertTrue(notPending.contains(accInative));
        Assert.assertTrue(notPending.contains(accLocked));
        Assert.assertFalse(notPending.contains(accPending));
    }
}
