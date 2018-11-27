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
package fr.cnes.regards.framework.modules.plugins.rest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Controller for REST Access to Plugin entities
 * @author Christophe Mertz
 * @author Sébastien Binda
 */
@RestController
public class PluginController implements IResourceController<PluginConfiguration> {

    /**
     * REST mapping resource : /plugins
     */
    public static final String PLUGINS = "/plugins";

    /**
     * REST mapping resource : /plugintypes
     */
    public static final String PLUGIN_TYPES = "/plugintypes";

    public static final String REQUEST_PARAM_PLUGIN_ID = "pluginId";

    /**
     * REST mapping resource : /plugins/{pluginId}
     */
    public static final String PLUGINS_PLUGINID = PLUGINS + "/{" + REQUEST_PARAM_PLUGIN_ID + "}";

    /**
     * REST mapping resource : /plugins/{pluginId}/config
     */
    public static final String PLUGINS_PLUGINID_CONFIGS = PLUGINS_PLUGINID + "/config";

    /**
     * REST mapping resource : /plugins/configs
     */
    public static final String PLUGINS_CONFIGS = PLUGINS + "/configs";

    public static final String REQUEST_PARAM_CONFIG_ID = "configId";

    /**
     * REST mapping resource : /plugins/{pluginId}/config/{configId}
     */
    public static final String PLUGINS_PLUGINID_CONFIGID =
            PLUGINS_PLUGINID_CONFIGS + "/{" + REQUEST_PARAM_CONFIG_ID + "}";

    /**
     * REST mapping resource : /plugins/configs/{configId}
     */
    public static final String PLUGINS_CONFIGID = PLUGINS_CONFIGS + "/{" + REQUEST_PARAM_CONFIG_ID + "}";

    /**
     * REST mapping resource : /plugins/cache
     */
    public static final String PLUGINS_CACHE = PLUGINS + "/cache";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginController.class);

    /**
     * Business service for Plugin.
     */
    private final IPluginService pluginService;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Constructor to specify a particular {@link IPluginService}.
     * @param pPluginService The {@link PluginService} used
     */
    public PluginController(IPluginService pPluginService) {
        super();
        pluginService = pPluginService;
    }

    /**
     * Get all the plugins identifies by the annotation {@link Plugin}.
     * @param pluginType a type of plugin
     * @return a {@link List} of {@link PluginMetaData}
     * @throws EntityInvalidException if problem occurs
     */
    @RequestMapping(value = PLUGINS, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(
            description = "Get all the class annotaded with @Plugin or only the one that implemented an optional pluginType",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<List<Resource<PluginMetaData>>> getPlugins(
            @RequestParam(value = "pluginType", required = false) String pluginType) throws EntityInvalidException {
        List<PluginMetaData> metadaData;

        if (pluginType == null) {
            // No plugintypes is specified, return all plugins
            metadaData = pluginService.getPlugins();
        } else {
            // A plugintypes is specified, return only plugins of this type
            try {
                metadaData = pluginService.getPluginsByType(Class.forName(pluginType));
            } catch (final ClassNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
                throw new EntityInvalidException("Class not found : " + e.getMessage());
            }
        }

        List<Resource<PluginMetaData>> resources = metadaData.stream().map(Resource::new).collect(Collectors.toList());

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Get the interface identified with the annotation {@link PluginInterface}.
     * @return a {@link List} of interface annotated with {@link PluginInterface}
     */
    @RequestMapping(value = PLUGIN_TYPES, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Get all the plugin types (ie interface annotated with @PluginInterface)")
    public ResponseEntity<List<Resource<String>>> getPluginTypes(
            @RequestParam(name = "available", required = false) Boolean available) {
        if ((available != null) && (available.booleanValue() == true)) {
            Set<String> types = pluginService.getAvailablePluginTypes();
            List<Resource<String>> resources = types.stream().map(Resource::new).collect(Collectors.toList());
            return new ResponseEntity<>(resources, HttpStatus.OK);
        }
        Set<String> types = pluginService.getPluginTypes();
        List<Resource<String>> resources = types.stream().map(Resource::new).collect(Collectors.toList());

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Get all the metadata of a specified plugin.
     * @param pluginId a plugin identifier
     * @return a {@link List} of {@link PluginParameter}
     */
    @RequestMapping(value = PLUGINS_PLUGINID, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Get the plugin Meta data for a specific plugin id", role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource<PluginMetaData>> getPluginMetaDataById(@PathVariable("pluginId") String pluginId) {
        PluginMetaData metaData = pluginService.getPluginMetaDataById(pluginId);
        Resource<PluginMetaData> resource = new Resource<>(metaData);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Get all the {@link PluginConfiguration} of a specified plugin.
     * @param pluginId a plugin identifier
     * @return a {@link List} of {@link PluginConfiguration}
     */
    @RequestMapping(value = PLUGINS_PLUGINID_CONFIGS, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Get all the plugin configuration for a specific plugin id",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<List<Resource<PluginConfiguration>>> getPluginConfigurations(
            @PathVariable("pluginId") String pluginId) {
        return ResponseEntity.ok(toResources(pluginService.getPluginConfigurations(pluginId)));
    }

    /**
     * Get all the {@link PluginConfiguration} for a specific plugin type.</br>
     * If any specific plugin type is defined, get all the {@link PluginConfiguration}.
     * @param pluginType an interface name, that implements {@link PluginInterface}.<br>
     * This parameter is optional.
     * @return a {@link List} of {@link PluginConfiguration}
     * @throws EntityNotFoundException the specific plugin type name is unknown
     */
    @RequestMapping(value = PLUGINS_CONFIGS, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Get all the plugin configuration for a specific type", role = DefaultRole.PUBLIC)
    public ResponseEntity<List<Resource<PluginConfiguration>>> getPluginConfigurationsByType(
            @RequestParam(value = "pluginType", required = false) String pluginType) throws EntityNotFoundException {

        List<PluginConfiguration> pluginConfs;

        if (pluginType != null) {
            // Get all the PluginConfiguration for a specific plugin type
            try {
                pluginConfs = pluginService.getPluginConfigurationsByType(Class.forName(pluginType));
            } catch (ClassNotFoundException e) {
                LOGGER.error("No class found for the plugin type :" + pluginType, e);
                throw new EntityNotFoundException(e.getMessage());
            }
        } else {
            // Get all the PluginConfiguration
            pluginConfs = pluginService.getAllPluginConfigurations();
        }

        return ResponseEntity.ok(toResources(pluginConfs));
    }

    /**
     * Create a new {@link PluginConfiguration}.
     * @param pluginConf a {@link PluginConfiguration}
     * @return the {@link PluginConfiguration] created
     * @throws ModuleException if problem occurs
     */
    @RequestMapping(value = PLUGINS_PLUGINID_CONFIGS, method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Create a plugin configuration")
    public ResponseEntity<Resource<PluginConfiguration>> savePluginConfiguration(
            @Valid @RequestBody PluginConfiguration pluginConf) throws ModuleException {
        try {
            return new ResponseEntity<>(toResource(pluginService.savePluginConfiguration(pluginConf)),
                                        HttpStatus.CREATED);
        } catch (final ModuleException e) {
            LOGGER.error("Cannot create the plugin configuration : <" + pluginConf.getPluginId() + ">", e);
            throw e;
        }
    }

    /**
     * Get the {@link PluginConfiguration} of a specified plugin.
     * @param pluginId a plugin identifier
     * @param configId a plugin configuration identifier
     * @return the {@link PluginConfiguration} of the plugin
     * @throws ModuleException the {@link PluginConfiguration} identified by the pConfigId parameter does not exists
     */
    @RequestMapping(value = PLUGINS_PLUGINID_CONFIGID, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Get a the plugin configuration of a specific plugin", role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource<PluginConfiguration>> getPluginConfiguration(
            @PathVariable("pluginId") String pluginId, @PathVariable("configId") Long configId) throws ModuleException {
        PluginConfiguration pluginConfig = pluginService.getPluginConfiguration(configId);
        return ResponseEntity.ok(toResource(pluginConfig));
    }

    /**
     * Get the {@link PluginConfiguration} of a specified plugin.
     * @param configId a plugin configuration identifier
     * @return the {@link PluginConfiguration} of the plugin
     * @throws ModuleException the {@link PluginConfiguration} identified by the pConfigId parameter does not exists
     */
    @RequestMapping(value = PLUGINS_CONFIGID, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Get a the plugin configuration", role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource<PluginConfiguration>> getPluginConfigurationDirectAccess(
            @PathVariable("configId") Long configId) throws ModuleException {
        return new ResponseEntity<>(new Resource<>(pluginService.getPluginConfiguration(configId)), HttpStatus.OK);
    }

    /**
     * Update a {@link PluginConfiguration} of a specified plugin.
     * @param pluginId a plugin identifier
     * @param configId a plugin configuration identifier
     * @param pluginConf a {@link PluginConfiguration}
     * @return the {@link PluginConfiguration} of the plugin.
     * @throws ModuleException the {@link PluginConfiguration} identified by the pConfigId parameter does not exists
     */
    @RequestMapping(value = PLUGINS_PLUGINID_CONFIGID, method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Update a plugin configuration")
    public ResponseEntity<Resource<PluginConfiguration>> updatePluginConfiguration(
            @PathVariable("pluginId") String pluginId, @PathVariable("configId") Long configId,
            @Valid @RequestBody PluginConfiguration pluginConf) throws ModuleException {

        if (!pluginId.equals(pluginConf.getPluginId())) {
            LOGGER.error("The plugin configuration is incoherent with the requests param : plugin id= <" + pluginId
                                 + ">- config id= <" + configId + ">");
            throw new EntityNotFoundException(pluginId, PluginConfiguration.class);
        }

        if (!configId.equals(pluginConf.getId())) {
            throw new EntityNotFoundException(configId.toString(), PluginConfiguration.class);
        }

        try {
            return ResponseEntity.ok(toResource(pluginService.updatePluginConfiguration(pluginConf)));
        } catch (final ModuleException e) {
            LOGGER.error("Cannot update the plugin configuration : <" + configId + ">", e);
            throw e;
        }
    }

    /**
     * Delete a {@link PluginConfiguration}.
     * @param pluginId a plugin identifier
     * @param configId a plugin configuration identifier
     * @return void response entity
     * @throws ModuleException the {@link PluginConfiguration} identified by the pConfigId parameter does not exists
     */
    @RequestMapping(value = PLUGINS_PLUGINID_CONFIGID, method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Delete a plugin configuration")
    public ResponseEntity<Void> deletePluginConfiguration(@PathVariable("pluginId") String pluginId,
            @PathVariable("configId") Long configId) throws ModuleException {
        pluginService.deletePluginConfiguration(configId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Empty cache
     */
    @RequestMapping(value = PLUGINS_CACHE, method = RequestMethod.DELETE)
    @ResourceAccess(description = "Empty plugins cache")
    public ResponseEntity<Void> emptyCache() {
        pluginService.cleanPluginCache();

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public Resource<PluginConfiguration> toResource(PluginConfiguration element, Object... extras) {
        Resource<PluginConfiguration> resource = null;
        if ((element != null) && (element.getId() != null)) {
            resource = resourceService.toResource(element);
            resourceService.addLink(resource, this.getClass(), "getPluginConfiguration", LinkRels.SELF,
                                    MethodParamFactory.build(String.class, element.getPluginId()),
                                    MethodParamFactory.build(Long.class, element.getId()));
            resourceService.addLink(resource, this.getClass(), "deletePluginConfiguration", LinkRels.DELETE,
                                    MethodParamFactory.build(String.class, element.getPluginId()),
                                    MethodParamFactory.build(Long.class, element.getId()));
            resourceService.addLink(resource, this.getClass(), "updatePluginConfiguration", LinkRels.UPDATE,
                                    MethodParamFactory.build(String.class, element.getPluginId()),
                                    MethodParamFactory.build(Long.class, element.getId()),
                                    MethodParamFactory.build(PluginConfiguration.class));
            resourceService.addLink(resource, this.getClass(), "getPluginConfigurations", LinkRels.LIST,
                                    MethodParamFactory.build(String.class, element.getPluginId()));
        }
        return resource;
    }

}
