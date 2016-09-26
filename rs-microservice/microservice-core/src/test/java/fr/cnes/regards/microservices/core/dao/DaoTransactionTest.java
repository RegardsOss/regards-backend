/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.microservices.core.dao.pojo.projects.User;
import fr.cnes.regards.microservices.core.dao.service.TransactionServiceTest;

/**
 *
 * Class DaoTransactionTest
 *
 * Test for transactionnal DAO actions
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { MultiTenancyDaoTestConfiguration.class })
@DirtiesContext
public class DaoTransactionTest {

    @Autowired
    private TransactionServiceTest service_;

    @Test
    public void transactionTest() {

        String testTenant = "test1";

        service_.deleteAll(testTenant);

        try {
            service_.addWithError("test1");
            Assert.fail("There must be an exception thrown");
        }
        catch (Exception e) {
            List<User> users = service_.getUsers(testTenant);
            Assert.assertTrue("Should be empty !", users.isEmpty());
        }

        service_.addWithoutError("test1");
        List<User> users = service_.getUsers(testTenant);
        Assert.assertTrue("there must be 1 element !", users.size() == 1);

    }

}
