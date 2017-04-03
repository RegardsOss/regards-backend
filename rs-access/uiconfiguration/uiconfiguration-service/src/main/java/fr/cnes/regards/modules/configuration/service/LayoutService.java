/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionCreatedEvent;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.configuration.dao.ILayoutRepository;
import fr.cnes.regards.modules.configuration.domain.Layout;
import fr.cnes.regards.modules.configuration.domain.LayoutDefaultApplicationIds;

/**
 *
 * Class LayoutService
 *
 * Service to manage Project IHM Layouts configurations
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Service(value = "layoutService")
@Transactional
public class LayoutService implements ILayoutService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(LayoutService.class);

    @Autowired
    private ILayoutRepository repository;

    /**
     * AMQP Message subscriber
     */
    @Autowired
    private IInstanceSubscriber instanceSubscriber;

    /**
     * The email validation template as html
     */
    @Value("classpath:DefaultApplicationLayout.json")
    private Resource defaultApplicationLayoutResource;

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

    @PostConstruct
    public void init() {
        // FIXME : not usefull for instance access / disable subscription
        instanceSubscriber.subscribeTo(TenantConnectionCreatedEvent.class, new TenantConnectionCreatedEventHandler());
        for (final String tenant : tenantResolver.getAllTenants()) {
            initProjectLayout(tenant);
        }
    }

    private class TenantConnectionCreatedEventHandler implements IHandler<TenantConnectionCreatedEvent> {

        @Override
        public void handle(final TenantWrapper<TenantConnectionCreatedEvent> pWrapper) {
            if (pWrapper.getContent().getMicroserviceName() == "rs-access") {
                initProjectLayout(pWrapper.getContent().getTenant().getTenant());
            }
        }

    }

    @Override
    public void initProjectLayout(final String pTenant) {
        try {
            final String layoutConf = readDefaultLayoutFileResource();
            final Layout layout = new Layout();
            layout.setApplicationId(LayoutDefaultApplicationIds.USER.toString());
            layout.setLayout(layoutConf);
            runtimeTenantResolver.forceTenant(pTenant);

            try {
                retrieveLayout(LayoutDefaultApplicationIds.USER.toString());
            } catch (final EntityNotFoundException e) {
                repository.save(layout);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Error reading layout default configuration file", e);
        }
    }

    @Override
    public Layout retrieveLayout(final String pApplicationId) throws EntityNotFoundException {
        return repository.findByApplicationId(pApplicationId)
                .orElseThrow(() -> new EntityNotFoundException(pApplicationId, Layout.class));
    }

    @Override
    public Layout saveLayout(final Layout pLayout) throws EntityAlreadyExistsException, EntityInvalidException {
        if (repository.findByApplicationId(pLayout.getApplicationId()).isPresent()) {
            throw new EntityAlreadyExistsException(pLayout.getApplicationId());
        }
        // Check layut json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pLayout.getLayout(), Object.class);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException("Layout is not a valid json format.");
        }
        return repository.save(pLayout);
    }

    @Override
    public Layout updateLayout(final Layout pLayout) throws EntityNotFoundException, EntityInvalidException {

        // Check layut json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pLayout.getLayout(), Object.class);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException("Layout is not a valid json format.");
        }
        if (!repository.exists(pLayout.getId())) {
            throw new EntityNotFoundException(pLayout.getId(), Layout.class);
        }
        return repository.save(pLayout);
    }

    /**
     *
     * Read the default Layout configuration file as a string.
     *
     * @return {@link Layout} as a string
     * @throws IOException
     * @since 1.0-SNAPSHOT
     */
    private String readDefaultLayoutFileResource() throws IOException {
        if ((defaultApplicationLayoutResource == null) || !defaultApplicationLayoutResource.exists()) {
            throw new RuntimeException(
                    "Error reading layout default configuration file" + defaultApplicationLayoutResource.getFilename());
        }
        try (BufferedReader buffer = new BufferedReader(
                new InputStreamReader(defaultApplicationLayoutResource.getInputStream()))) {
            return buffer.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

}
