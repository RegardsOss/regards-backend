/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.exception.DaoTestException;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.pojo.User;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.repository.IUserRepository;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.utils.CurrentTenantIdentifierResolverMock;

/**
 *
 * Test service for transactionnal DAO actions
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Service
public class DaoUserService {

    /**
     * User name used to simulate creation of user with error.
     */
    private static final String USER_NAME_ERROR = "doNotSave";

    /**
     * User last name used to simulate creation of user with error.
     */
    private static final String USER_LAST_NAME_ERROR = "ThisUser";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DaoUserService.class);

    /**
     * JPA User repository
     */
    @Autowired
    private IUserRepository userRepository;

    /**
     * Mock tenant resolver to set tenant manually instead of reading it from SecurityContext JWT Token.
     */
    @Autowired
    private CurrentTenantIdentifierResolverMock tenantResolver;

    /**
     *
     * Test adding a user with error. Rollback must be done.
     *
     * @param pTenant
     *            Tenant or project to use
     * @throws DaoTestException
     *             Simulated error always thrown to activate JPA rollback
     * @since 1.0-SNAPSHOT
     */
    @Transactional(transactionManager = "projectsJpaTransactionManager", rollbackFor = DaoTestException.class)
    public void addWithError(String pTenant) throws DaoTestException {
        final String message = "new user created id=";
        tenantResolver.setTenant(pTenant);
        User plop = userRepository.save(new User(USER_NAME_ERROR, USER_LAST_NAME_ERROR));
        LOG.info(message + plop.getId());
        plop = userRepository.save(new User(USER_NAME_ERROR, USER_LAST_NAME_ERROR));
        LOG.info(message + plop.getId());
        plop = userRepository.save(new User(USER_NAME_ERROR, USER_LAST_NAME_ERROR));
        LOG.info(message + plop.getId());
        throw new DaoTestException("Generated test error to check for dao rollback");

    }

    /**
     *
     * Test adding a user without error
     *
     * @param pTenant
     *            Tenant or project to use
     * @since 1.0-SNAPSHOT
     */
    public void addWithoutError(String pTenant) {
        tenantResolver.setTenant(pTenant);
        final User plop = userRepository.save(new User("valid", "thisUser"));
        LOG.info("New user created id=" + plop.getId());
    }

    /**
     *
     * Test getting all users from a given tenant
     *
     * @param pTenant
     *            Tenant or project to use
     * @return Result list of users
     * @since 1.0-SNAPSHOT
     */
    public List<User> getUsers(String pTenant) {
        tenantResolver.setTenant(pTenant);
        final Iterable<User> list = userRepository.findAll();
        final List<User> results = new ArrayList<>();
        list.forEach(user -> results.add(user));
        return results;
    }

    /**
     *
     * Test method to delete all users from a given tenant
     *
     * @param pTenant
     *            Tenant or project to use
     * @since 1.0-SNAPSHOT
     */
    public void deleteAll(String pTenant) {
        tenantResolver.setTenant(pTenant);
        userRepository.deleteAll();
    }

}
