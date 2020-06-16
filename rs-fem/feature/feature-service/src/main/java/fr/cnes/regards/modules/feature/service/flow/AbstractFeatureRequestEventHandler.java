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
package fr.cnes.regards.modules.feature.service.flow;

import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.service.IFeatureDeniedService;

/**
 * @author Marc SORDI
 */
public abstract class AbstractFeatureRequestEventHandler<M> implements IBatchHandler<M> {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    private final Class<M> type;

    public AbstractFeatureRequestEventHandler(Class<M> type) {
        this.type = type;
    }

    @Override
    public Class<M> getMType() {
        return type;
    }

    @Override
    public boolean handleConversionError(String tenant, Message message, String errorMessage) {
        try {
            runtimeTenantResolver.forceTenant(tenant);
            return getFeatureService().denyMessage(getFeatureRequestType(), message, errorMessage);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    public abstract IFeatureDeniedService getFeatureService();

    public abstract FeatureRequestType getFeatureRequestType();
}
