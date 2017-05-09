/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionReady;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.configuration.domain.Layout;
import fr.cnes.regards.modules.configuration.service.exception.MissingResourceException;

/**
 *
 * Class AbstractUiConfigurationService
 *
 * Abstract class for all rs-access microservice services. Allow to define a specific init method at start-up for
 * multintenant and instance microservices.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public abstract class AbstractUiConfigurationService {

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
    private boolean isMultitenentMicroservice;

    /**
     * Init
     */
    @PostConstruct
    public void init() {
        if (isMultitenentMicroservice) {
            // Multitenant version of the microservice.
            // Initialize subscriber for new tenant connection and initialize database if not already done
            instanceSubscriber.subscribeTo(TenantConnectionReady.class, new TenantConnectionReadyEventHandler());
            for (final String tenant : tenantResolver.getAllActiveTenants()) {
                runtimeTenantResolver.forceTenant(tenant);
                initProjectUI(tenant);
            }
        } else {
            // Initialize database if not already done
            initInstanceUI();
        }
    }

    private class TenantConnectionReadyEventHandler implements IHandler<TenantConnectionReady> {

        @Override
        public void handle(final TenantWrapper<TenantConnectionReady> pWrapper) {
            if (microserviceName.equals(pWrapper.getContent().getMicroserviceName())) {
                runtimeTenantResolver.forceTenant(pWrapper.getContent().getTenant());
                initProjectUI(pWrapper.getContent().getTenant());
            }
        }

    }

    /**
     * Read the default Layout configuration file as a string.
     *
     * @return {@link Layout} as a string
     * @throws IOException
     * @since 1.0-SNAPSHOT
     */
    protected String readDefaultFileResource(final Resource pResource) throws IOException {

        if ((pResource == null) || !pResource.exists()) {
            throw new MissingResourceException();
        }
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(pResource.getInputStream()))) {
            return buffer.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    protected abstract void initProjectUI(String pTenant);

    protected abstract void initInstanceUI();

}
