/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.rest;

import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 * Controller for REST Access to Plugin entities
 *
 * @author Christophe Mertz
 * @author Sébastien Binda
 *
 */
@RestController
@ModuleInfo(name = "plugins", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class PluginController implements IResourceController<PluginConfiguration> {

    /**
     * REST mapping resource : /plugins
     */
    public static final String PLUGINS = "/plugins";

    /**
     * REST mapping resource : /plugintypes
     */
    public static final String PLUGIN_TYPES = "/plugintypes";

    /**
     * REST mapping resource : /plugins/{pluginId}
     */
    public static final String PLUGINS_PLUGINID = "/plugins/{pluginId}";

    /**
     * REST mapping resource : /plugins/{pluginId}/config
     */
    public static final String PLUGINS_CONFIGS = "/plugins/{pluginId}/config";

    /**
     * REST mapping resource : /plugins/{pluginId}/config/{configId}
     */
    public static final String PLUGINS_CONFIGID = "/plugins/{pluginId}/config/{configId}";

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
     *
     * @param pPluginService
     *            The {@link PluginService} used
     */
    public PluginController(final IPluginService pPluginService) {
        super();
        pluginService = pPluginService;
    }

    /**
     * Get all the plugins identifies by the annotation {@link Plugin}.
     *
     * @param pPluginType
     *            a type of plugin
     *
     * @return a list of {@link PluginMetaData}
     *
     * @throws EntityInvalidException
     *             if problem occurs
     */
    @RequestMapping(value = PluginController.PLUGINS, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(
            description = "Get all the class annotaded with @Plugin or only the one that implemented an optional pluginType")
    public ResponseEntity<List<Resource<PluginMetaData>>> getPlugins(
            @RequestParam(value = "pluginType", required = false) final String pPluginType)
            throws EntityInvalidException {
        List<PluginMetaData> metadaDatas;

        if (pPluginType == null) {
            // No plugintypes is specify, return all the plugins
            metadaDatas = pluginService.getPlugins();

        } else {
            // A plugintypes is specify, return only the plugin of this plugin type
            try {
                metadaDatas = pluginService.getPluginsByType(Class.forName(pPluginType));
            } catch (final ClassNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
                throw new EntityInvalidException(e.getMessage());
            }
        }

        final List<Resource<PluginMetaData>> resources = metadaDatas.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Get the interface identified with the annotation {@link PluginInterface}.
     *
     * @return a list of interface annotated with {@link PluginInterface}.
     */
    @RequestMapping(value = PluginController.PLUGIN_TYPES, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Get all the plugin types (ie interface annotated with @PluginInterface)")
    public ResponseEntity<List<Resource<String>>> getPluginTypes() {
        final List<String> types = pluginService.getPluginTypes();
        final List<Resource<String>> resources = types.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Get all the metadata of a specified plugin.
     *
     * @param pPluginId
     *            a plugin identifier
     *
     * @return a list of {@link PluginParameter}
     */
    @RequestMapping(value = PluginController.PLUGINS_PLUGINID, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Get the plugin Meta data for a specific plugin id")
    public ResponseEntity<Resource<PluginMetaData>> getPluginMetaDataById(
            @PathVariable("pluginId") final String pPluginId) {
        final PluginMetaData metaData = pluginService.getPluginMetaDataById(pPluginId);
        final Resource<PluginMetaData> resource = new Resource<>(metaData);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Get all the {@link PluginConfiguration} of a specified plugin.
     *
     * @param pPluginId
     *            a plugin identifier
     *
     * @return a list of {@link PluginConfiguration}
     */
    @RequestMapping(value = PluginController.PLUGINS_CONFIGS, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Get all the plugin configuration for a specific plugin id")
    public ResponseEntity<List<Resource<PluginConfiguration>>> getPluginConfigurations(
            @PathVariable("pluginId") final String pPluginId) {
        final List<PluginConfiguration> pluginConfs = pluginService.getPluginConfigurationsByType(pPluginId);
        return ResponseEntity.ok(toResources(pluginConfs));
    }

    /**
     * Create a new {@link PluginConfiguration}.
     *
     * @param pPluginConfiguration
     *            a {@link PluginConfiguration}
     *
     * @return the {@link PluginConfiguration] created
     *
     * @throws EntityInvalidException
     *             if problem occurs
     */
    @RequestMapping(value = PluginController.PLUGINS_CONFIGS, method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Create a plugin configuration")
    public ResponseEntity<Resource<PluginConfiguration>> savePluginConfiguration(
            @Valid @RequestBody final PluginConfiguration pPluginConfiguration) throws EntityInvalidException {
        PluginConfiguration pluginConfig;
        try {
            pluginConfig = pluginService.savePluginConfiguration(pPluginConfiguration);
        } catch (final PluginUtilsException e) {
            LOGGER.error("Cannot create the plugin configuration : <" + pPluginConfiguration.getPluginId() + ">", e);
            throw new EntityInvalidException(e.getMessage());
        }

        return new ResponseEntity<>(toResource(pluginConfig), HttpStatus.CREATED);
    }

    /**
     * Get the {@link PluginConfiguration} of a specified plugin.
     *
     * @param pPluginId
     *            a plugin identifier
     *
     * @param pConfigId
     *            a plugin configuration identifier
     *
     * @return the {@link PluginConfiguration} of the plugin
     *
     * @throws EntityNotFoundException
     *             the {@link PluginConfiguration} identified by the pConfigId parameter does not exists
     *
     */
    @RequestMapping(value = PluginController.PLUGINS_CONFIGID, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Get a the plugin configuration")
    public ResponseEntity<Resource<PluginConfiguration>> getPluginConfiguration(
            @PathVariable("pluginId") final String pPluginId, @PathVariable("configId") final Long pConfigId)
            throws EntityNotFoundException {
        PluginConfiguration pluginConfig;
        try {
            pluginConfig = pluginService.getPluginConfiguration(pConfigId);
        } catch (final PluginUtilsException e) {
            LOGGER.error("Cannot get the plugin configuration : <" + pConfigId + ">", e);
            throw new EntityNotFoundException(pConfigId, PluginConfiguration.class);
        }

        return ResponseEntity.ok(toResource(pluginConfig));
    }

    /**
     * Update a {@link PluginConfiguration} of a specified plugin.
     *
     * @param pPluginId
     *            a plugin identifier
     *
     * @param pConfigId
     *            a plugin configuration identifier
     *
     * @param pPluginConfiguration
     *            a {@link PluginConfiguration}
     *
     * @return the {@link PluginConfiguration} of the plugin.
     *
     * @throws EntityNotFoundException
     *             the {@link PluginConfiguration} identified by the pConfigId parameter does not exists
     */
    @RequestMapping(value = PluginController.PLUGINS_CONFIGID, method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Update a plugin configuration")
    public ResponseEntity<Resource<PluginConfiguration>> updatePluginConfiguration(
            @PathVariable("pluginId") final String pPluginId, @PathVariable("configId") final Long pConfigId,
            @Valid @RequestBody final PluginConfiguration pPluginConfiguration) throws EntityNotFoundException {

        if (!pPluginId.equals(pPluginConfiguration.getPluginId())) {
            LOGGER.error("The plugin configuration is incoherent with the requests param : plugin id= <" + pPluginId
                    + ">- config id= <" + pConfigId + ">");
            throw new EntityNotFoundException(pPluginId, PluginConfiguration.class);
        }

        if (pConfigId != pPluginConfiguration.getId()) {
            throw new EntityNotFoundException(pConfigId.toString(), PluginConfiguration.class);
        }

        PluginConfiguration pluginConfig;

        try {
            pluginConfig = pluginService.updatePluginConfiguration(pPluginConfiguration);
        } catch (final PluginUtilsException e) {
            LOGGER.error("Cannot update the plugin configuration : <" + pConfigId + ">", e);
            throw new EntityNotFoundException(pConfigId, PluginConfiguration.class);
        }

        return ResponseEntity.ok(toResource(pluginConfig));
    }

    /**
     * Delete a {@link PluginConfiguration}.
     *
     * @param pPluginId
     *            a plugin identifier
     *
     * @param pConfigId
     *            a plugin configuration identifier
     *
     * @return void
     *
     * @throws EntityNotFoundException
     *             the {@link PluginConfiguration} identified by the pConfigId parameter does not exists
     */
    @RequestMapping(value = PluginController.PLUGINS_CONFIGID, method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Delete a plugin configuration")
    public ResponseEntity<Void> deletePluginConfiguration(@PathVariable("pluginId") final String pPluginId,
            @PathVariable("configId") final Long pConfigId) throws EntityNotFoundException {
        pluginService.deletePluginConfiguration(pConfigId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public Resource<PluginConfiguration> toResource(PluginConfiguration pElement, Object... pExtras) {
        Resource<PluginConfiguration> resource = null;
        if ((pElement != null) && (pElement.getId() != null)) {
            resource = resourceService.toResource(pElement);
            resourceService.addLink(resource, this.getClass(), "getPluginConfiguration", LinkRels.SELF,
                                    MethodParamFactory.build(String.class, pElement.getPluginId()),
                                    MethodParamFactory.build(Long.class, pElement.getId()));
            resourceService.addLink(resource, this.getClass(), "deletePluginConfiguration", LinkRels.DELETE,
                                    MethodParamFactory.build(String.class, pElement.getPluginId()),
                                    MethodParamFactory.build(Long.class, pElement.getId()));
            resourceService.addLink(resource, this.getClass(), "updatePluginConfiguration", LinkRels.UPDATE,
                                    MethodParamFactory.build(String.class, pElement.getPluginId()),
                                    MethodParamFactory.build(Long.class, pElement.getId()),
                                    MethodParamFactory.build(PluginConfiguration.class));
            resourceService.addLink(resource, this.getClass(), "getPluginConfigurations", LinkRels.LIST,
                                    MethodParamFactory.build(String.class, pElement.getPluginId()));
        }
        return resource;
    }

}
