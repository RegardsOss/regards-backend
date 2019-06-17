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
package fr.cnes.regards.framework.logbackappender;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.logbackappender.domain.LogEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * @author Christophe Mertz
 */
public class RegardsAmqpAppender extends AppenderBase<ILoggingEvent> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RegardsAmqpAppender.class);

    /**
     * Unknow user. No user authenticated (JWT Token).
     */
    private static final String UNDEFINED_USER = "unknown";

    /**
     * The {@link Publisher} used to send {@link LogEvent}
     */
    private final IPublisher publisher;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * The microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    public RegardsAmqpAppender(IPublisher publisher) {
        super();
        this.publisher = publisher;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {

        String user = authResolver.getUser();
        if (user == null) {
            user = UNDEFINED_USER;
        }
        String tenant = runtimeTenantResolver.getTenant();

        if (tenant != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[" + tenant + "] <" + microserviceName + "> send message  <" + eventObject
                        .getFormattedMessage() + ">");
            }

            Instant instant = Instant.ofEpochMilli(eventObject.getTimeStamp());
            LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

            final LogEvent sended = new LogEvent(eventObject.getFormattedMessage(), microserviceName,
                                                 eventObject.getCallerData()[0].getClassName(),
                                                 eventObject.getCallerData()[0].getMethodName(), ldt.toString(),
                                                 eventObject.getLevel().toString(), user);
            publisher.publish(sended);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[" + tenant + "] message sended : " + sended.toString());
            }
        }
    }

}
