/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.service;

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
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.access.dao.instance.IInstanceLayoutRepository;
import fr.cnes.regards.modules.access.domain.LayoutDefaultApplicationIds;
import fr.cnes.regards.modules.access.domain.instance.InstanceLayout;
import fr.cnes.regards.modules.access.domain.project.Layout;

/**
 *
 * Class InstanceLayoutService
 *
 * Service to manage layouts for instance entities (Portal IHM)
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPHOT
 */
@Service(value = "instanceLayoutService")
public class InstanceLayoutService implements IInstanceLayoutService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(InstanceLayoutService.class);

    @Autowired
    private IInstanceLayoutRepository repository;

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
        try {
            final String layoutConf = new String(
                    Files.readAllBytes(Paths.get(defaultApplicationLayoutResource.getURI())));
            final InstanceLayout layout = new InstanceLayout();
            layout.setApplicationId(LayoutDefaultApplicationIds.PORTAL.toString());
            layout.setLayout(layoutConf);

            try {
                retrieveLayout(LayoutDefaultApplicationIds.PORTAL.toString());
            } catch (final EntityNotFoundException e) {
                repository.save(layout);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Error reading layout default configuration file", e);
        }
    }

    @Override
    public InstanceLayout retrieveLayout(final String pApplicationId) throws EntityNotFoundException {
        return repository.findByApplicationId(pApplicationId)
                .orElseThrow(() -> new EntityNotFoundException(pApplicationId, Layout.class));
    }

    @Override
    public InstanceLayout saveLayout(final InstanceLayout pLayout) throws EntityAlreadyExistsException {
        if (repository.findByApplicationId(pLayout.getApplicationId()).isPresent()) {
            throw new EntityAlreadyExistsException(pLayout.getApplicationId());
        }
        return repository.save(pLayout);
    }

    @Override
    public InstanceLayout updateLayout(final InstanceLayout pLayout)
            throws EntityNotFoundException, EntityInvalidException {

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
