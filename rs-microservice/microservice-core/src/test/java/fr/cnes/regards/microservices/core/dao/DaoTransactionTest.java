/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.microservices.core.dao.pojo.projects.User;
import fr.cnes.regards.microservices.core.dao.service.DaoUserTest;

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

    static final Logger LOG = LoggerFactory.getLogger(DaoTransactionTest.class);

    @Autowired
    private DaoUserTest service_;

    @Test
    public void transactionTest() {

        String testTenant = "test1";
        String testTenant2 = "test2";
        List<User> users = new ArrayList<>();

        // Delete any datas
        service_.deleteAll(testTenant);
        service_.deleteAll(testTenant2);

        // Add a user with transaction error. There must be a rollback and nothing commited.
        try {
            LOG.info("Adding users to first tenant with exception thrown ... ");
            service_.addWithError(testTenant);
            Assert.fail("There must be an exception thrown");
        } catch (Exception e) {
            LOG.error(e.getMessage());
            users.clear();
            users = service_.getUsers(testTenant);
            Assert.assertTrue("The first tenant should be empty !", users.isEmpty());
            LOG.info("DAO Rollback correctly done !");
        }

        // Add valid user
        LOG.info("Adding valid users to first tenant ... ");
        service_.addWithoutError(testTenant);
        users.clear();
        users = service_.getUsers(testTenant);
        Assert.assertTrue("There must be 1 element !", users.size() == 1);
        LOG.info("Insert correctly done and commited ! ");

        // Test for second tenant
        users.clear();
        users = service_.getUsers(testTenant2);
        Assert.assertTrue("There must be 0 elements !", users.isEmpty());

        // Add a user with transaction error. There must be a rollback and nothing commited.
        try {
            LOG.info("Adding users to second tenant with exception thrown ... ");
            service_.addWithError(testTenant2);
            Assert.fail("There must be an exception thrown");
        } catch (Exception e) {
            LOG.error(e.getMessage());
            users.clear();
            users = service_.getUsers(testTenant2);
            Assert.assertTrue("The second tenant should be empty !", users.isEmpty());
            LOG.info("DAO Rollback correctly done !");
        }

        // Add valid users to second tenant
        LOG.info("Adding valid users to second tenant ... ");
        service_.addWithoutError(testTenant2);
        service_.addWithoutError(testTenant2);
        users.clear();
        users = service_.getUsers(testTenant2);
        Assert.assertTrue("There must be 2 elements !", users.size() == 2);
        LOG.info("Inserts correctly done and commited ! ");

        // Check that the first tenant hasn't changed.
        users.clear();
        users = service_.getUsers(testTenant);
        Assert.assertTrue("There must be 1 element ! " + users.size(), users.size() == 1);

    }

}
