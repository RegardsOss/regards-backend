/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.plugins.service.PluginService;
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
public class PluginController {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginController.class);

    /**
     * Business service for Plugin.
     */
    private final IPluginService pluginService;

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
    @RequestMapping(value = "/plugins", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Resource<PluginMetaData>>> getPlugins(
            @RequestParam(value = "pluginType", required = false) final String pPluginType)
            throws EntityInvalidException {
        final List<PluginMetaData> metadaDatas;

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
    @RequestMapping(value = "/plugintypes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
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
    @RequestMapping(value = "/plugins/{pluginId}", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
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
    @RequestMapping(value = "/plugins/{pluginId}/config", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Resource<PluginConfiguration>>> getPluginConfigurations(
            @PathVariable("pluginId") final String pPluginId) {
        final List<PluginConfiguration> pluginConfs = pluginService.getPluginConfigurationsByType(pPluginId);
        final List<Resource<PluginConfiguration>> resources = pluginConfs.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
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
    @RequestMapping(value = "/plugins/{pluginId}/config", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Resource<PluginConfiguration>> savePluginConfiguration(
            @Valid @RequestBody final PluginConfiguration pPluginConfiguration) throws EntityInvalidException {
        final PluginConfiguration pluginConfiguration;
        try {
            pluginConfiguration = pluginService.savePluginConfiguration(pPluginConfiguration);
        } catch (final PluginUtilsException e) {
            LOGGER.error("Cannot create the plugin configuration : <" + pPluginConfiguration.getPluginId() + ">", e);
            throw new EntityInvalidException(e.getMessage());
        }
        final Resource<PluginConfiguration> resource = new Resource<>(pluginConfiguration);

        return new ResponseEntity<>(resource, HttpStatus.CREATED);
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
    @RequestMapping(value = "/plugins/{pluginId}/config/{configId}", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Resource<PluginConfiguration>> getPluginConfiguration(
            @PathVariable("pluginId") final String pPluginId, @PathVariable("configId") final Long pConfigId)
            throws EntityNotFoundException {
        final PluginConfiguration pluginConfiguration;
        try {
            pluginConfiguration = pluginService.getPluginConfiguration(pConfigId);
        } catch (final PluginUtilsException e) {
            LOGGER.error("Cannot get the plugin configuration : <" + pConfigId + ">", e);
            throw new EntityNotFoundException(pConfigId, PluginConfiguration.class);
        }
        final Resource<PluginConfiguration> resource = new Resource<>(pluginConfiguration);

        return new ResponseEntity<>(resource, HttpStatus.OK);
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
    @RequestMapping(value = "/plugins/{pluginId}/config/{configId}", method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Resource<PluginConfiguration>> updatePluginConfiguration(
            @PathVariable("pluginId") final String pPluginId, @PathVariable("configId") final Long pConfigId,
            @Valid @RequestBody final PluginConfiguration pPluginConfiguration) throws EntityNotFoundException {
        final PluginConfiguration pluginConfiguration;
        if (!pPluginId.equals(pPluginConfiguration.getPluginId())) {
            LOGGER.error("The plugin configuration is incoherent with the requests param : plugin id= <" + pPluginId
                    + ">- config id= <" + pConfigId + ">");
            throw new EntityNotFoundException(pPluginId, PluginConfiguration.class);
        }
        if (pConfigId != pPluginConfiguration.getId()) {
            throw new EntityNotFoundException(pConfigId.toString(), PluginConfiguration.class);
        }
        try {
            pluginConfiguration = pluginService.updatePluginConfiguration(pPluginConfiguration);
        } catch (final PluginUtilsException e) {
            LOGGER.error("Cannot update the plugin configuration : <" + pConfigId + ">", e);
            throw new EntityNotFoundException(pConfigId, PluginConfiguration.class);
        }
        final Resource<PluginConfiguration> resource = new Resource<>(pluginConfiguration);

        return new ResponseEntity<>(resource, HttpStatus.OK);
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
    @RequestMapping(value = "/plugins/{pluginId}/config/{configId}", method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Void> deletePluginConfiguration(@PathVariable("pluginId") final String pPluginId,
            @PathVariable("configId") final Long pConfigId) throws EntityNotFoundException {
        pluginService.deletePluginConfiguration(pConfigId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
