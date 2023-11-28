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

import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.dao.IModuleRepository;
import fr.cnes.regards.modules.configuration.dao.ModuleSpecifications;
import fr.cnes.regards.modules.configuration.domain.LayoutDefaultApplicationIds;
import fr.cnes.regards.modules.configuration.domain.Module;
import fr.cnes.regards.modules.configuration.domain.UIPage;
import fr.cnes.regards.modules.configuration.service.exception.InitUIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Class ModuleService
 * <p>
 * Service to manage modules entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Service
@RegardsTransactional
public class ModuleService extends AbstractUiConfigurationService implements IModuleService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ModuleService.class);

    private static final String MODULE_HAS_NOT_VALID_JSON_FORMAT = "Module has not valid json format.";

    public static final String LAYERS = "layers";

    public static final String CONTENT = "content";

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
     * The default configuration for project user entity description
     */
    @Value("classpath:DefaultUserDescriptionModule.json")
    private Resource defaultUserDescriptionModuleResource;

    /**
     * The default configuration for portal menu
     */
    @Value("classpath:DefaultPortalMenu.json")
    private Resource defaultPortalMenuResource;

    @Autowired
    private IModuleRepository repository;

    @Autowired
    private SearchHistoryService searchHistoryService;

    @Override
    public Module retrieveModule(final Long moduleId) throws EntityNotFoundException {
        final Optional<Module> module = repository.findById(moduleId);
        if (!module.isPresent()) {
            throw new EntityNotFoundException(moduleId, Module.class);
        }
        return module.get();
    }

    @Override
    public Page<Module> retrieveModules(final String applicationId,
                                        Boolean active,
                                        String type,
                                        final Pageable pPageable) {
        return repository.findAll(ModuleSpecifications.search(applicationId, active, type), pPageable);
    }

    @Override
    public Page<Module> retrieveModules(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Page<Module> retrieveActiveModules(final String applicationId, final Pageable pageable) {
        return repository.findByApplicationIdAndActiveTrue(applicationId, pageable);
    }

    @Override
    public Module saveModule(final Module module) throws EntityInvalidException {
        // Check module configuration json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(module.getConf(), Object.class);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException(MODULE_HAS_NOT_VALID_JSON_FORMAT, e);
        }
        UIPage page = module.getPage();
        if ((page != null) && page.isHome()) {
            disableHomeForAllApplicationModules(module.getApplicationId());
        }
        return repository.save(module);
    }

    @Override
    public Module updateModule(final Module module) throws EntityException {
        // Check layut json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(module.getConf(), Object.class);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException(MODULE_HAS_NOT_VALID_JSON_FORMAT, e);
        }
        if (!repository.existsById(module.getId())) {
            throw new EntityNotFoundException(module.getId(), Module.class);
        }
        UIPage page = module.getPage();
        if ((page != null) && page.isHome()) {
            disableHomeForAllApplicationModules(module.getApplicationId());
        }
        return repository.save(module);
    }

    @Override
    public void deleteModule(final Long moduleId) throws EntityNotFoundException {
        if (!repository.existsById(moduleId)) {
            throw new EntityNotFoundException(moduleId, Module.class);
        }
        repository.deleteById(moduleId);
        searchHistoryService.deleteModuleSearchHistory(moduleId);
    }

    /**
     * Set to false the defaultDynamicModule attribute of all modules for the given application id
     *
     * @since 1.0-SNAPSHOT
     */
    private void disableHomeForAllApplicationModules(final String applicationId) {
        final List<Module> modules = repository.findByApplicationIdAndPageHomeTrue(applicationId);
        for (final Module module : modules) {
            if ((module.getPage() != null) && module.getPage().isHome()) {
                module.getPage().setHome(false);
            }
            repository.save(module);
        }
    }

    @Override
    protected void initProjectUI(final String tenant) {

        if (repository.findByApplicationId(LayoutDefaultApplicationIds.USER.toString()).isEmpty()) {
            final Module menu = new Module();
            menu.setActive(true);
            menu.setApplicationId(LayoutDefaultApplicationIds.USER.toString());
            menu.setContainer("page-top-header");
            menu.setDescription(String.format("%s menu", tenant));
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
            catalog.setContainer("page-content-module");
            catalog.setDescription("Catalog");
            catalog.setType("search-results");
            catalog.setPage(new UIPage(false, "DEFAULT", null, "{\"en\":\"Catalog\",\"fr\":\"Catalogue\"}"));
            try {
                catalog.setConf(readDefaultFileResource(defaultUserCatalogModuleResource));
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                throw new InitUIException(e);
            }
            repository.save(catalog);

            final Module description = new Module();
            description.setActive(true);
            description.setApplicationId(LayoutDefaultApplicationIds.USER.toString());
            description.setContainer("page-top-header");
            description.setDescription("Entities description");
            description.setType("description");
            try {
                description.setConf(readDefaultFileResource(defaultUserDescriptionModuleResource));
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                throw new InitUIException(e);
            }
            repository.save(description);
        }

    }

    @Override
    protected void initInstanceUI() {
        if (repository.findByApplicationId(LayoutDefaultApplicationIds.PORTAL.toString()).isEmpty()) {
            final Module menu = new Module();
            menu.setActive(true);
            menu.setApplicationId(LayoutDefaultApplicationIds.PORTAL.toString());
            menu.setContainer("header");
            menu.setDescription("Portal menu");
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
            projectList.setContainer(CONTENT);
            projectList.setDescription("List of projects");
            projectList.setType("projects-list");
            projectList.setConf("{}");
            repository.save(projectList);
        }
    }

}
