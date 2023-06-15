/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional.pojo.User;
import fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional.repository.IUserRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Test service for transactionnal DAO actions
 *
 * @author CS
 */
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DaoUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaoUserService.class);

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
    private final IUserRepository userRepository;

    /**
     * JWT service
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final DaoUserService self;

    public DaoUserService(IUserRepository userRepository,
                          IRuntimeTenantResolver runtimeTenantResolver,
                          DaoUserService self) {
        this.userRepository = userRepository;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.self = self;
    }

    /**
     * Test adding a user with error. Rollback must be done.
     *
     * @param pTenant Tenant or project to use
     * @throws DaoTestException Simulated error always thrown to activate JPA rollback
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

    public void incrementUserCountWithRetryWithOptimistic(Long userId, String tenant) {
        try {
            runtimeTenantResolver.forceTenant(tenant);
            self.incrementUserCountWithOptimisticLock(userId);
        } catch (ObjectOptimisticLockingFailureException e) {
            LOGGER.info("Optimistic locking failure, retrying");
            incrementUserCountWithRetryWithOptimistic(userId, tenant);
        } catch (CannotCreateTransactionException e) {
            LOGGER.info("Connection failure, retrying");
            try {
                // Add a sleep time to avoid too many connection attempts. With embededd datasource this can happen
                // during disk file access.
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                LOGGER.error(e.getMessage(), e);
                throw new RuntimeException(ex);
            }
            incrementUserCountWithRetryWithOptimistic(userId, tenant);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementUserCountWithOptimisticLock(Long userId) {
        LOGGER.info("Incrementing user count ...");
        User user = userRepository.findByIdOptimisticLock(userId).get();
        LOGGER.info("Incrementing user count from {} to {}", user.getCount(), user.getCount() + 1);
        user.incrementCounter();
        userRepository.save(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementUserCountWithPessimisticLock(Long userId) {
        User user = userRepository.findByIdPessimisticReadLock(userId).get();
        LOGGER.info("Incrementing user count from {} to {}", user.getCount(), user.getCount() + 1);
        user.incrementCounter();
        userRepository.save(user);
    }

    public void incrementUserCountWithPessimisticLock(Long userId, String tenant) {
        runtimeTenantResolver.forceTenant(tenant);
        self.incrementUserCountWithPessimisticLock(userId);
    }

    /**
     * Test adding a user without error
     *
     * @param pTenant Tenant or project to use
     */
    public void addWithoutError(final String pTenant) {
        runtimeTenantResolver.forceTenant(pTenant);
        final User plop = userRepository.save(new User("valid", "thisUser"));
        LOG.info("New user created id=" + plop.getId());
    }

    /**
     * Test getting all users from a given tenant
     *
     * @param pTenant Tenant or project to use
     * @return Result list of users
     */
    public List<User> getUsers(final String pTenant) {
        runtimeTenantResolver.forceTenant(pTenant);
        final Iterable<User> list = userRepository.findAll();
        final List<User> results = new ArrayList<>();
        list.forEach(results::add);
        return results;
    }

    /**
     * Test method to delete all users from a given tenant
     *
     * @param pTenant Tenant or project to use
     */
    public void deleteAll(final String pTenant) {
        runtimeTenantResolver.forceTenant(pTenant);
        userRepository.deleteAll();
    }

}
