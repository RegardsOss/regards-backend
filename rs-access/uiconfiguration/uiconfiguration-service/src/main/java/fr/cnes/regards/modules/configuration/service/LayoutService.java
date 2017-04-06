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
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionReady;
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
    @Value("classpath:DefaultUserApplicationLayout.json")
    private Resource defaultUserApplicationLayoutResource;

    /**
     * The email validation template as html
     */
    @Value("classpath:DefaultPortalApplicationLayout.json")
    private Resource defaultPortalApplicationLayoutResource;

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

    @Value("${spring.application.name}")
    private String microserviceName;

    @Value("${regards.access.multitenant:true}")
    private boolean isMultitenentMicroservice;

    @PostConstruct
    public void init() {
        if (isMultitenentMicroservice) {
            // Multitenant version of the microservice.
            // Initialize subscriber for new tenant connection and initialize database if not already done
            instanceSubscriber.subscribeTo(TenantConnectionReady.class, new TenantConnectionReadyEventHandler());
            for (final String tenant : tenantResolver.getAllTenants()) {
                initProjectLayout(tenant);
            }
        } else {
            // Initialize database if not already done
            initInstanceLayout();
        }
    }

    private class TenantConnectionReadyEventHandler implements IHandler<TenantConnectionReady> {

        @Override
        public void handle(final TenantWrapper<TenantConnectionReady> pWrapper) {
            if (microserviceName.equals(pWrapper.getContent().getMicroserviceName())) {
                initProjectLayout(pWrapper.getContent().getTenant());
            }
        }

    }

    public void initInstanceLayout() {
        try {
            final String layoutConf = readDefaultLayoutFileResource();
            final Layout layout = new Layout();
            layout.setApplicationId(LayoutDefaultApplicationIds.PORTAL.toString());
            layout.setLayout(layoutConf);

            try {
                retrieveLayout(LayoutDefaultApplicationIds.PORTAL.toString());
            } catch (final EntityNotFoundException e) {
                LOG.info("Initializing new layout for Portal application");
                repository.save(layout);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Error reading layout default configuration file", e);
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
                LOG.info("Initializing new layout for User application (project {})", pTenant);
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
        Resource resource;
        if (isMultitenentMicroservice) {
            // For multintenant the initialization is for User applications.
            resource = defaultUserApplicationLayoutResource;
        } else {
            // For instance the initialization is for Portal applications.
            resource = defaultPortalApplicationLayoutResource;
        }
        if ((resource == null) || !resource.exists()) {
            throw new RuntimeException("Error reading layout default configuration file" + resource.getFilename());
        }
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            return buffer.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

}
