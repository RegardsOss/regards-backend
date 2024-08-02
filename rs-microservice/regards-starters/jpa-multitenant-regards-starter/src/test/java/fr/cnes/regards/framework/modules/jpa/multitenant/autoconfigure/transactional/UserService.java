/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;

/**
 * @author Sébastien Binda
 **/
@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private DaoUserService daoUserService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Retryable(value = { OptimisticLockingFailureException.class, CannotCreateTransactionException.class },
               maxAttempts = 50000,
               backoff = @Backoff(delay = 1))
    public void incrementUserCount(Long userId, String tenant) {
        runtimeTenantResolver.forceTenant(tenant);
        daoUserService.incrementUserCountWithOptimisticLock(userId);
    }

    public void incrementUserCountWithPessimisticLock(Long userId, String tenant) {
        runtimeTenantResolver.forceTenant(tenant);
        daoUserService.incrementUserCountWithPessimisticLock(userId);
    }

    @Recover
    public void recoverIncrementUserCount(Exception e, Long userId, String tenant) {
        LOGGER.error("Too many retry for user incrementation count. Optimistic lock is maybe not the right solution "
                     + "here");
    }

}
