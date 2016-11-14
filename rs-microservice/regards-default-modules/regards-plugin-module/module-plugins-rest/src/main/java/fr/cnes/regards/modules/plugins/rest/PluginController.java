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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.plugins.service.PluginService;
import fr.cnes.regards.modules.plugins.signature.IPluginsSignature;
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
public class PluginController implements IPluginsSignature {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginController.class);

    /**
     * Business service for Project entities.
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

    @Override
    public ResponseEntity<List<Resource<PluginMetaData>>> getPlugins(
            @RequestParam(value = "pluginType", required = false) final String pPluginType)
            throws InvalidValueException {
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
                throw new InvalidValueException(e.getMessage());
            }
        }

        final List<Resource<PluginMetaData>> resources = metadaDatas.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Resource<String>>> getPluginTypes() {
        final List<String> types = pluginService.getPluginTypes();
        final List<Resource<String>> resources = types.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<PluginMetaData>> getPluginMetaDataById(
            @PathVariable("pluginId") final String pPluginId) {
        final PluginMetaData metaData = pluginService.getPluginMetaDataById(pPluginId);
        final Resource<PluginMetaData> resource = new Resource<>(metaData);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Resource<PluginConfiguration>>> getPluginConfigurations(
            @PathVariable("pluginId") final String pPluginId) {
        final List<PluginConfiguration> pluginConfs = pluginService.getPluginConfigurationsByType(pPluginId);
        final List<Resource<PluginConfiguration>> resources = pluginConfs.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<PluginConfiguration>> savePluginConfiguration(
            @Valid @RequestBody final PluginConfiguration pPluginConfiguration) throws InvalidValueException {
        final PluginConfiguration pluginConfiguration;
        try {
            pluginConfiguration = pluginService.savePluginConfiguration(pPluginConfiguration);
        } catch (final PluginUtilsException e) {
            LOGGER.error("Cannot create the plugin configuration : <" + pPluginConfiguration.getPluginId() + ">", e);
            throw new InvalidValueException(e.getMessage());
        }
        final Resource<PluginConfiguration> resource = new Resource<>(pluginConfiguration);

        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Resource<PluginConfiguration>> getPluginConfiguration(
            @PathVariable("pluginId") final String pPluginId, @PathVariable("configId") final Long pConfigId)
            throws ModuleEntityNotFoundException {
        final PluginConfiguration pluginConfiguration;
        try {
            pluginConfiguration = pluginService.getPluginConfiguration(pConfigId);
        } catch (final PluginUtilsException e) {
            LOGGER.error("Cannot get the plugin configuration : <" + pConfigId + ">", e);
            throw new ModuleEntityNotFoundException(pConfigId.toString(), PluginConfiguration.class);
        }
        final Resource<PluginConfiguration> resource = new Resource<>(pluginConfiguration);

        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<PluginConfiguration>> updatePluginConfiguration(
            @PathVariable("pluginId") final String pPluginId, @PathVariable("configId") final Long pConfigId,
            @Valid @RequestBody final PluginConfiguration pPluginConfiguration) throws ModuleEntityNotFoundException {
        final PluginConfiguration pluginConfiguration;
        if (!pPluginId.equals(pPluginConfiguration.getPluginId())) {
            LOGGER.error("The plugin configuration is incoherent with the requests param : plugin id= <" + pPluginId
                    + ">- config id= <" + pConfigId + ">");
            throw new ModuleEntityNotFoundException(pPluginId, PluginConfiguration.class);
        }
        if (pConfigId != pPluginConfiguration.getId()) {
            throw new ModuleEntityNotFoundException(pConfigId.toString(), PluginConfiguration.class);
        }
        try {
            pluginConfiguration = pluginService.updatePluginConfiguration(pPluginConfiguration);
        } catch (final PluginUtilsException e) {
            LOGGER.error("Cannot update the plugin configuration : <" + pConfigId + ">", e);
            throw new ModuleEntityNotFoundException(pConfigId.toString(), PluginConfiguration.class);
        }
        final Resource<PluginConfiguration> resource = new Resource<>(pluginConfiguration);

        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deletePluginConfiguration(@PathVariable("pluginId") final String pPluginId,
            @PathVariable("configId") final Long pConfigId) throws ModuleEntityNotFoundException {
        try {
            pluginService.deletePluginConfiguration(pConfigId);
        } catch (final PluginUtilsException e) {
            LOGGER.error("Cannot delete the plugin configuration : <" + pConfigId + ">", e);
            throw new ModuleEntityNotFoundException(pConfigId.toString(), PluginConfiguration.class);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
