/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.transactional;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.transactional.pojo.User;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.transactional.repository.IUserRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

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
     * JWT service
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     *
     * Test adding a user with error. Rollback must be done.
     *
     * @param pTenant
     *            Tenant or project to use
     * @throws DaoTestException
     *             Simulated error always thrown to activate JPA rollback
     * @throws MissingClaimException
     * @throws InvalidJwtException
     * @since 1.0-SNAPSHOT
     */
    @Transactional(transactionManager = "multitenantsJpaTransactionManager", rollbackFor = DaoTestException.class)
    public void addWithError(final String pTenant) throws DaoTestException {
        final String message = "new user created id=";
        runtimeTenantResolver.forceTenant(pTenant);
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
     * @throws MissingClaimException
     * @throws InvalidJwtException
     * @since 1.0-SNAPSHOT
     */
    public void addWithoutError(final String pTenant) {
        runtimeTenantResolver.forceTenant(pTenant);
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
     * @throws MissingClaimException
     * @throws InvalidJwtException
     * @since 1.0-SNAPSHOT
     */
    public List<User> getUsers(final String pTenant) {
        runtimeTenantResolver.forceTenant(pTenant);
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
     * @throws MissingClaimException
     * @throws InvalidJwtException
     * @since 1.0-SNAPSHOT
     */
    public void deleteAll(final String pTenant) {
        runtimeTenantResolver.forceTenant(pTenant);
        userRepository.deleteAll();
    }

}
