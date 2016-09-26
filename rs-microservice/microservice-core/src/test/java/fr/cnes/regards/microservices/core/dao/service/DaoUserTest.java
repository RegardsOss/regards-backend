/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.microservices.core.dao.pojo.projects.User;
import fr.cnes.regards.microservices.core.dao.repository.projects.UserRepository;
import fr.cnes.regards.microservices.core.dao.util.CurrentTenantIdentifierResolverMock;

/**
 *
 * Test service for transactionnal DAO actions
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class DaoUserTest {

    static final Logger LOG = LoggerFactory.getLogger(DaoUserTest.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrentTenantIdentifierResolverMock tenantResolver;

    /**
     *
     * Test adding a user witho error. Rollback must be done.
     *
     * @param pTenant
     * @since 1.0-SNAPSHOT
     */
    @Transactional(transactionManager = "projectsJpaTransactionManager", rollbackFor = Exception.class)
    public void addWithError(String pTenant) throws Exception {
        tenantResolver.setTenant(pTenant);
        User plop = userRepository.save(new User("doNotSave", "thisUser"));
        LOG.info("new user created id=" + plop.getId());
        plop = userRepository.save(new User("doNotSave", "thisUser"));
        LOG.info("new user created id=" + plop.getId());
        plop = userRepository.save(new User("doNotSave", "thisUser"));
        LOG.info("new user created id=" + plop.getId());
        throw new Exception("Generated test error to check for dao rollback");

    }

    /**
     *
     * Test adding a user without error
     *
     * @param pTenant
     * @since 1.0-SNAPSHOT
     */
    public void addWithoutError(String pTenant) {
        tenantResolver.setTenant(pTenant);
        User plop = userRepository.save(new User("doNotSave", "thisUser"));
        LOG.info("New user created id=" + plop.getId());
    }

    /**
     *
     * Test getting all users from a given tenant
     *
     * @param pTenant
     * @since 1.0-SNAPSHOT
     */
    public List<User> getUsers(String pTenant) {
        tenantResolver.setTenant(pTenant);
        Iterable<User> list = userRepository.findAll();
        List<User> results = new ArrayList<>();
        list.forEach(user -> results.add(user));
        return results;
    }

    /**
     *
     * Test methid to delete all users from a given tenant
     *
     * @param pTenant
     * @since 1.0-SNAPSHOT
     */
    public void deleteAll(String pTenant) {
        tenantResolver.setTenant(pTenant);
        userRepository.deleteAll();
    }

}
