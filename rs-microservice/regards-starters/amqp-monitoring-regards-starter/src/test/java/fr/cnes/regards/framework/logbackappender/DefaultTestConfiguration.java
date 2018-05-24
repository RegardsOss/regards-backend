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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.logbackappender.domain.ILogEventHandler;
import fr.cnes.regards.framework.logbackappender.domain.LogEvent;

/**
 * Default Amqp Regards Appender test configuration
 *
 * @author Christophe Mertz
 *
 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:amqp-rabbit.properties")
public class DefaultTestConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTestConfiguration.class);

    private List<TenantWrapper<LogEvent>> wrappers = new ArrayList<>();

    @Bean
    public ILogEventHandler logEventHandler() {
        return new LogEventHandlerTest();
    }

    @Bean(name="receiverLogEvent")
    public SubscriberLogEvent createSubscriberLogEvent() {
        return new SubscriberLogEvent();
    }

    @Autowired
    private SubscriberLogEvent receiverLogEvent;

    /**
     * This class is used to test the publish and subscribe of the {@link LogEvent}.
     * 
     * @author Christophe Mertz
     *
     */
    private class LogEventHandlerTest implements ILogEventHandler {

        private Boolean lock = Boolean.TRUE;

        public void handle(TenantWrapper<LogEvent> pWrapper) {
            LOGGER.debug("a new event received : [" + pWrapper.getTenant() + "] - <" + pWrapper.getContent().getMsg()+">");
            receiverLogEvent.addLogEvent(pWrapper);
        }

    }
}
