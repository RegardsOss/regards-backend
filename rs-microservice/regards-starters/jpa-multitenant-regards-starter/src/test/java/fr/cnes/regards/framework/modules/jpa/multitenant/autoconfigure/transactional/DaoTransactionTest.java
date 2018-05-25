/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional;

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

import fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional.pojo.User;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

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
@ContextConfiguration(classes = { DaoTransactionTestConfiguration.class })
@DirtiesContext
public class DaoTransactionTest {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DaoTransactionTest.class);

    /**
     * User service
     */
    @Autowired
    private DaoUserService service;

    /**
     *
     * Test for multitenant transactions.
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test multitenant transactions operations in database")
    @Test
    public void transactionTest() {

        final String testTenant = "test1";
        final String testTenant2 = "test2";
        final String exceptionError = "There must be an exception thrown";
        final String rollbackSucceed = "DAO Rollback correctly done !";
        List<User> users = new ArrayList<>();

        // Delete any datas
        service.deleteAll(testTenant);
        service.deleteAll(testTenant2);

        // Add a user with transaction error. There must be a rollback and nothing commited.
        try {
            LOG.info("Adding users to first tenant with exception thrown ... ");
            service.addWithError(testTenant);
            Assert.fail(exceptionError);
        } catch (final DaoTestException e) {
            LOG.error(e.getMessage());
            users.clear();
            users = service.getUsers(testTenant);
            Assert.assertTrue("The first tenant should be empty !", users.isEmpty());
            LOG.info(rollbackSucceed);
        }

        // Add valid user
        LOG.info("Adding valid users to first tenant ... ");
        service.addWithoutError(testTenant);
        users.clear();
        users = service.getUsers(testTenant);
        Assert.assertTrue("There must be 1 element !", users.size() == 1);
        LOG.info("Insert correctly done and commited ! ");

        // Test for second tenant
        users.clear();
        users = service.getUsers(testTenant2);
        Assert.assertTrue("There must be 0 elements !", users.isEmpty());

        // Add a user with transaction error. There must be a rollback and nothing commited.
        try {
            LOG.info("Adding users to second tenant with exception thrown ... ");
            service.addWithError(testTenant2);
            Assert.fail(exceptionError);
        } catch (final DaoTestException e) {
            LOG.error(e.getMessage());
            users.clear();
            users = service.getUsers(testTenant2);
            Assert.assertTrue("The second tenant should be empty !", users.isEmpty());
            LOG.info(rollbackSucceed);
        }

        // Add valid users to second tenant
        LOG.info("Adding valid users to second tenant ... ");
        service.addWithoutError(testTenant2);
        service.addWithoutError(testTenant2);
        users.clear();
        users = service.getUsers(testTenant2);
        Assert.assertTrue("There must be 2 elements !", users.size() == 2);
        LOG.info("Inserts correctly done and commited ! ");

        // Check that the first tenant hasn't changed.
        users.clear();
        users = service.getUsers(testTenant);
        Assert.assertTrue("There must be 1 element ! " + users.size(), users.size() == 1);
    }

}
