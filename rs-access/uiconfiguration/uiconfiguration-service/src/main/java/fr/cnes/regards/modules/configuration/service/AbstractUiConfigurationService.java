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
package fr.cnes.regards.modules.configuration.service;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.configuration.domain.UILayout;
import fr.cnes.regards.modules.configuration.service.exception.MissingResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Class AbstractUiConfigurationService
 * <p>
 * Abstract class for all rs-access microservice services. Allow to define a specific init method at start-up for
 * multintenant and instance microservices.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public abstract class AbstractUiConfigurationService implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Tenant resolver to access all configured tenant
     */
    @Autowired
    private ITenantResolver tenantResolver;

    /**
     * AMQP Message subscriber
     */
    @Autowired
    private IInstanceSubscriber instanceSubscriber;

    @Value("${spring.application.name}")
    private String microserviceName;

    @Value("${regards.access.multitenant:true}")
    private boolean isMultitenantMicroservice;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (isMultitenantMicroservice) {
            // Multitenant version of the microservice.
            for (final String tenant : tenantResolver.getAllActiveTenants()) {
                runtimeTenantResolver.forceTenant(tenant);
                initProjectUI(tenant);
                runtimeTenantResolver.clearTenant();
            }
        } else {
            // Initialize database if not already done
            initInstanceUI();
        }
    }

    /**
     * Read the default Layout configuration file as a string.
     *
     * @return {@link UILayout} as a string
     * @throws IOException
     * @since 1.0-SNAPSHOT
     */
    protected String readDefaultFileResource(final Resource resource) throws IOException {

        if ((resource == null) || !resource.exists()) {
            throw new MissingResourceException();
        }
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            return buffer.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    protected abstract void initProjectUI(String pTenant);

    protected abstract void initInstanceUI();

    /**
     * @return the instanceSubscriber
     */
    public IInstanceSubscriber getInstanceSubscriber() {
        return instanceSubscriber;
    }

    /**
     * @return the runtimeTenantResolver
     */
    public IRuntimeTenantResolver getRuntimeTenantResolver() {
        return runtimeTenantResolver;
    }

    /**
     * @return the microserviceName
     */
    public String getMicroserviceName() {
        return microserviceName;
    }

}
