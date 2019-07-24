/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.dao.optim;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;

/**
 * @author Marc SORDI
 *
 */
public class ConcurrentTasks extends Thread {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private TransactionalService transactionalService;

    private String tenant;

    private List<AIPEntity> concurrentlyUpdatedEntities;

    public ConcurrentTasks(String tenant, List<AIPEntity> concurrentlyUpdatedEntities) {
        this.tenant = tenant;
        this.concurrentlyUpdatedEntities = concurrentlyUpdatedEntities;
    }

    @Override
    public void run() {
        runtimeTenantResolver.forceTenant(tenant);
        for (AIPEntity entity : concurrentlyUpdatedEntities) {
            entity.setState(AIPState.DELETED);
            transactionalService.update(entity);
        }
    }
}
