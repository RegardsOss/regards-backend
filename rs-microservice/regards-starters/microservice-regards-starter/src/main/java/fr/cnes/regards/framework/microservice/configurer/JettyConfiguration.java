/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.microservice.configurer;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author oroussel
 */
@Configuration
public class JettyConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(JettyConfiguration.class);

    @Value("${jetty.threadPool.idleTimeout:3600000}")
    private int threadPoolIdleTimeout;

    @Bean
    public EmbeddedServletContainerCustomizer customizer() {
        LOGGER.info("Customizing Jetty server...");
        return new EmbeddedServletContainerCustomizer() {

            @Override
            public void customize(ConfigurableEmbeddedServletContainer container) {
                if (container instanceof JettyEmbeddedServletContainerFactory) {
                    customizeJetty((JettyEmbeddedServletContainerFactory) container);
                }
            }

            private void customizeJetty(JettyEmbeddedServletContainerFactory jetty) {
                QueuedThreadPool threadPool = new QueuedThreadPool();
                LOGGER.info("Setting Jetty server thread pool idle timeout to {} ms", threadPoolIdleTimeout);
                threadPool.setIdleTimeout(threadPoolIdleTimeout);
                jetty.setThreadPool(threadPool);
                jetty.addServerCustomizers((JettyServerCustomizer) server -> {
                    for (Connector connector : server.getConnectors()) {
                        if (connector instanceof ServerConnector) {
                            ((ServerConnector) connector).setIdleTimeout(threadPoolIdleTimeout);
                        }
                    }
                });
            }
        };
    }
}
