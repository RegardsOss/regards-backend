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

package fr.cnes.regards.modules.metrics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.logbackappender.RegardsAmqpAppender;
import fr.cnes.regards.framework.logbackappender.domain.ILogEventHandler;
import fr.cnes.regards.framework.logbackappender.domain.LogEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.metrics.dao.ILogEventRepository;
import fr.cnes.regards.modules.metrics.domain.LogEventJpa;

/**
 * This class defines the handler executed for each {@link LogEventJpa} send by each microservice through the
 * {@link RegardsAmqpAppender} used in logger.
 *
 * @author Christophe Mertz
 *
 */
@Service
@MultitenantTransactional
public class LogEventHandler implements ILogEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogEvent.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ILogEventRepository logEventRepository;

    public LogEventHandler(IRuntimeTenantResolver runtimeTenantResolver, ILogEventRepository logEventRepository) {
        LOGGER.debug("Creation bean of type <LogEventHandler>");
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.logEventRepository = logEventRepository;
    }

    @Override
    public void handle(TenantWrapper<LogEvent> pWrapper) {
        try {
            LogEvent logEvent = pWrapper.getContent();
            LOGGER.debug("[" + pWrapper.getTenant() + "] a new event received : " + logEvent.toString());

            runtimeTenantResolver.forceTenant(pWrapper.getTenant());
            LogEventJpa logEventToSave = new LogEventJpa(pWrapper.getContent());
            logEventRepository.save(logEventToSave);
        } catch (RuntimeException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

}