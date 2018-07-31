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
package fr.cnes.regards.modules.configuration.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

/**
 *
 * Class ModuleService
 *
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
    public Page<Module> retrieveModules(final String applicationId, Boolean active, String type,
            final Pageable pPageable) {
        return repository.findAll(ModuleSpecifications.search(applicationId, active, type), pPageable);
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
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException("Module is not a valid json format.", e);
        }
        UIPage page = pModule.getPage();
        if ((page != null) && page.isHome()) {
            disableHomeForAllApplicationModules(pModule.getApplicationId());
        }
        return repository.save(pModule);
    }

    @Override
    public Module updateModule(final Module pModule) throws EntityException {
        // Check layut json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pModule.getConf(), Object.class);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException("Module is not a valid json format.", e);
        }
        if (!repository.exists(pModule.getId())) {
            throw new EntityNotFoundException(pModule.getId(), Module.class);
        }
        UIPage page = pModule.getPage();
        if ((page != null) && page.isHome()) {
            disableHomeForAllApplicationModules(pModule.getApplicationId());
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

    @Override
    public JsonObject addDatasetLayersInsideModuleConf(Module module, JsonObject dataset, String openSearchLink) throws EntityInvalidException {
        final Gson gson = new Gson();
        JsonObject moduleConfJson;

        try {
            JsonElement element = gson.fromJson(module.getConf(), JsonElement.class);
            moduleConfJson = element.getAsJsonObject();
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException("Module is not a valid json format.", e);
        }

        JsonArray layers = new JsonArray();
        if (!dataset.has("content")) {
            LOG.warn("There is no dataset available for this user");
            return moduleConfJson;
        }
        JsonArray ds = dataset.getAsJsonArray("content");
        // Iterate over datasets resources
        ds.forEach(d -> {
            String datasetIpId = d.getAsJsonObject().get("content").getAsJsonObject().get("ipId").getAsString();
            JsonObject layer = new JsonObject();
            layer.addProperty("category", "Catalog");
            layer.addProperty("type", "OpenSearch");
            layer.addProperty("baseUrl", openSearchLink.replace("DATASET_ID", datasetIpId));
            layer.addProperty("visible", false);
            layers.add(layer);
        });
        //  Add to the end of the list all layers configured in the module json
        moduleConfJson.get("layers").getAsJsonArray().forEach(layer -> {
            layers.add(layer);
        });
        // save the layer list inside the module conf
        moduleConfJson.add("layers", layers);
        return moduleConfJson;
    }

    /**
     *
     * Set to false the defaultDynamicModule attribute of all modules for the given application id
     *
     * @param pApplicationId
     * @since 1.0-SNAPSHOT
     */
    private void disableHomeForAllApplicationModules(final String pApplicationId) {
        final List<Module> modules = repository.findByApplicationIdAndPageHomeTrue(pApplicationId);
        for (final Module module : modules) {
            if ((module.getPage() != null) && module.getPage().isHome()) {
                module.getPage().setHome(false);
            }
            repository.save(module);
        }
    }

    @Override
    protected void initProjectUI(final String pTenant) {

        if (repository.findByApplicationId(LayoutDefaultApplicationIds.USER.toString()).isEmpty()) {
            final Module menu = new Module();
            menu.setActive(true);
            menu.setApplicationId(LayoutDefaultApplicationIds.USER.toString());
            menu.setContainer("page-top-header");
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
        }

    }

    @Override
    protected void initInstanceUI() {
        if (repository.findByApplicationId(LayoutDefaultApplicationIds.PORTAL.toString()).isEmpty()) {
            final Module menu = new Module();
            menu.setActive(true);
            menu.setApplicationId(LayoutDefaultApplicationIds.PORTAL.toString());
            menu.setContainer("header");
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
            projectList.setDescription("List of projects");
            projectList.setType("projects-list");
            projectList.setConf("{}");
            repository.save(projectList);
        }
    }

}
