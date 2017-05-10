/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.dao.IModuleRepository;
import fr.cnes.regards.modules.configuration.domain.LayoutDefaultApplicationIds;
import fr.cnes.regards.modules.configuration.domain.Module;
import fr.cnes.regards.modules.configuration.service.exception.InitUIException;

/**
 *
 * Class ModuleService
 *
 * Service to manage modules entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Service(value = "moduleService")
@Transactional
public class ModuleService extends AbstractUiConfigurationService implements IModuleService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ModuleService.class);

    /**
     * The default configuration for project user menu
     */
    @Value("classpath:DefaultUserMenu.json")
    private Resource defaultUserMenuResource;

    /**
     * The default configuration for project user catalog
     */
    @Value("classpath:DefaultUserCatalogModule.json")
    private Resource defaultUserCatalogModuleResource;

    /**
     * The default configuration for portal menu
     */
    @Value("classpath:DefaultPortalMenu.json")
    private Resource defaultPortalMenuResource;

    @Autowired
    private IModuleRepository repository;

    @Override
    public Module retrieveModule(final Long pModuleId) throws EntityNotFoundException {
        final Module module = repository.findOne(pModuleId);
        if (module == null) {
            throw new EntityNotFoundException(pModuleId, Module.class);
        }
        return module;
    }

    @Override
    public Page<Module> retrieveModules(final String pApplicationId, final Pageable pPageable) {
        return repository.findByApplicationId(pApplicationId, pPageable);
    }

    @Override
    public Page<Module> retrieveActiveModules(final String pApplicationId, final Pageable pPageable) {
        return repository.findByApplicationIdAndActiveTrue(pApplicationId, pPageable);
    }

    @Override
    public Module saveModule(final Module pModule) throws EntityInvalidException {
        // Check module configuration json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pModule.getConf(), Object.class);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException("Layout is not a valid json format.");
        }
        if (pModule.isDefaultDynamicModule()) {
            disableDefaultForAllApplicationModules(pModule.getApplicationId());
        }
        return repository.save(pModule);
    }

    @Override
    public Module updateModule(final Module pModule) throws EntityNotFoundException, EntityInvalidException {
        // Check layut json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pModule.getConf(), Object.class);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException("Layout is not a valid json format.");
        }
        if (!repository.exists(pModule.getId())) {
            throw new EntityNotFoundException(pModule.getId(), Module.class);
        }
        if (pModule.isDefaultDynamicModule()) {
            disableDefaultForAllApplicationModules(pModule.getApplicationId());
        }
        return repository.save(pModule);
    }

    @Override
    public void deleteModule(final Long pModuleId) throws EntityNotFoundException {
        if (!repository.exists(pModuleId)) {
            throw new EntityNotFoundException(pModuleId, Module.class);
        }
        repository.delete(pModuleId);

    }

    /**
     *
     * Set to false the defaultDynamicModule attribute of all modules for the given application id
     *
     * @param pApplicationId
     * @since 1.0-SNAPSHOT
     */
    private void disableDefaultForAllApplicationModules(final String pApplicationId) {
        final List<Module> modules = repository.findByApplicationIdAndDefaultDynamicModuleTrue(pApplicationId);
        for (final Module module : modules) {
            module.setDefaultDynamicModule(false);
            repository.save(module);
        }
    }

    @Override
    protected void initProjectUI(final String pTenant) {

        if (repository.findByApplicationId(LayoutDefaultApplicationIds.USER.toString()).isEmpty()) {
            final Module menu = new Module();
            menu.setActive(true);
            menu.setApplicationId(LayoutDefaultApplicationIds.USER.toString());
            menu.setContainer("header");
            menu.setDefaultDynamicModule(false);
            menu.setDescription(String.format("%s menu", pTenant));
            menu.setType("menu");
            try {
                menu.setConf(readDefaultFileResource(defaultUserMenuResource));
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                throw new InitUIException(e);
            }
            repository.save(menu);

            final Module catalog = new Module();
            catalog.setActive(true);
            catalog.setApplicationId(LayoutDefaultApplicationIds.USER.toString());
            catalog.setContainer("dynamic-content");
            catalog.setDefaultDynamicModule(true);
            catalog.setDescription("Catalog");
            catalog.setType("search-results");
            try {
                catalog.setConf(readDefaultFileResource(defaultUserCatalogModuleResource));
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                throw new InitUIException(e);
            }
            repository.save(catalog);
        }

    }

    @Override
    protected void initInstanceUI() {
        if (repository.findByApplicationId(LayoutDefaultApplicationIds.PORTAL.toString()).isEmpty()) {
            final Module menu = new Module();
            menu.setActive(true);
            menu.setApplicationId(LayoutDefaultApplicationIds.PORTAL.toString());
            menu.setContainer("header");
            menu.setDefaultDynamicModule(false);
            menu.setDescription(String.format("Portal menu"));
            menu.setType("menu");
            try {
                menu.setConf(readDefaultFileResource(defaultPortalMenuResource));
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                throw new InitUIException(e);
            }
            repository.save(menu);

            final Module projectList = new Module();
            projectList.setActive(true);
            projectList.setApplicationId(LayoutDefaultApplicationIds.PORTAL.toString());
            projectList.setContainer("content");
            projectList.setDefaultDynamicModule(false);
            projectList.setDescription("List of projects");
            projectList.setType("projects-list");
            projectList.setConf("{}");
            repository.save(projectList);
        }
    }

}
