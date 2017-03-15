/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.configuration.dao.ILayoutRepository;
import fr.cnes.regards.modules.configuration.domain.Layout;
import fr.cnes.regards.modules.configuration.domain.LayoutDefaultApplicationIds;
import fr.cnes.regards.modules.project.domain.event.NewProjectConnectionEvent;

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
    private ISubscriber subscriber;

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
        subscriber.subscribeTo(NewProjectConnectionEvent.class, new NewProjectConnectionEventHandler(this));
        for (final String tenant : tenantResolver.getAllTenants()) {
            initProjectLayout(tenant);
        }
    }

    private class NewProjectConnectionEventHandler implements IHandler<NewProjectConnectionEvent> {

        private final ILayoutService layoutService;

        public NewProjectConnectionEventHandler(final ILayoutService pLayoutService) {
            super();
            layoutService = pLayoutService;
        }

        @Override
        public void handle(final TenantWrapper<NewProjectConnectionEvent> pWrapper) {
            if (pWrapper.getContent().getNewProjectConnection().getMicroservice() == "rs-access") {
                layoutService.initProjectLayout(pWrapper.getContent().getNewProjectConnection().getProject().getName());
            }
        }

    }

    @Override
    public void initProjectLayout(final String pTenant) {
        try {
            final String layoutConf = new String(
                    Files.readAllBytes(Paths.get(defaultApplicationLayoutResource.getURI())));
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
    public Layout saveLayout(final Layout pLayout) throws EntityAlreadyExistsException {
        if (repository.findByApplicationId(pLayout.getApplicationId()).isPresent()) {
            throw new EntityAlreadyExistsException(pLayout.getApplicationId());
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

}
