/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityExistsException;
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

import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;
import fr.cnes.regards.modules.core.rest.AbstractController;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.plugins.signature.IPluginsSignature;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 * Controller for REST Access to Puugin entities
 * 
 * @author cmertz
 *
 */
@RestController
@ModuleInfo(name = "plugins", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class PluginController extends AbstractController implements IPluginsSignature {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginController.class);

    /**
     * Business service for Project entities. Autowired.
     */
    private IPluginService pluginService;

    /**
     * Constructor to specify a particular {@link IPluginService}.
     * 
     * @param pPluginService
     */
    public PluginController(final IPluginService pPluginService) {
        super();
        pluginService = pPluginService;
    }

    @Override
    public ResponseEntity<List<Resource<PluginMetaData>>> getPlugins(
            @RequestParam(value = "pluginType", required = false) final String pPluginType) throws EntityException {
        final List<PluginMetaData> metadaDatas;
        List<Resource<PluginMetaData>> resources;

        if (pPluginType == null) {
            // No plugintypes is specify, return all the plugins
            metadaDatas = pluginService.getPlugins();

        } else {
            // A plugintypes is specify, return only the plugin of this plugin type
            try {
                metadaDatas = pluginService.getPluginsByType(Class.forName(pPluginType));
            } catch (ClassNotFoundException e) {
                LOGGER.error(e.getMessage());
                throw new EntityExistsException(e);
            }
        }

        resources = metadaDatas.stream().map(p -> new Resource<>(p)).collect(Collectors.toList());

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Resource<String>>> getPluginTypes() {
        List<String> types = pluginService.getPluginTypes();
        List<Resource<String>> resources = types.stream().map(p -> new Resource<>(p)).collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<PluginMetaData>> getPluginMetaDataById(@PathVariable("pluginId") String pPluginId) {
        PluginMetaData metaData = pluginService.getPluginMetaDataById(pPluginId);
        final Resource<PluginMetaData> resource = new Resource<>(metaData);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Resource<PluginConfiguration>>> getPluginConfigurations(
            @PathVariable("pluginId") String pPluginId) {
        final List<PluginConfiguration> pluginConfs = pluginService.getPluginConfigurationsByType(pPluginId);
        List<Resource<PluginConfiguration>> resources = pluginConfs.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<PluginConfiguration>> savePluginConfiguration(
            @Valid @RequestBody PluginConfiguration pPluginConfiguration) throws InvalidValueException {
        PluginConfiguration pluginConfiguration;
        try {
            pluginConfiguration = pluginService.savePluginConfiguration(pPluginConfiguration);
        } catch (PluginUtilsException e) {
            LOGGER.error("Cannot create the plugin configuration : <" + pPluginConfiguration.getPluginId() + ">", e);
            throw new InvalidValueException(e.getMessage());
        }
        final Resource<PluginConfiguration> resource = new Resource<>(pluginConfiguration);

        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Resource<PluginConfiguration>> getPluginConfiguration(
            @PathVariable("pluginId") String pPluginId, @PathVariable("configId") Long pConfigId)
            throws EntityNotFoundException {
        PluginConfiguration pluginConfiguration;
        try {
            pluginConfiguration = pluginService.getPluginConfiguration(pConfigId);
        } catch (PluginUtilsException e) {
            LOGGER.error("Cannot get the plugin configuration : <" + pConfigId + ">", e);
            throw new EntityNotFoundException(pConfigId.toString(), PluginConfiguration.class);
        }
        final Resource<PluginConfiguration> resource = new Resource<>(pluginConfiguration);

        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<PluginConfiguration>> updatePluginConfiguration(
            @PathVariable("pluginId") String pPluginId, @PathVariable("configId") Long pConfigId,
            @Valid @RequestBody PluginConfiguration pPluginConfiguration)
            throws EntityNotFoundException, InvalidValueException {
        PluginConfiguration pluginConfiguration;
        if (pPluginId != pPluginConfiguration.getPluginId() || pConfigId != pPluginConfiguration.getId()) {
            LOGGER.error("coucou");
            throw new InvalidValueException(
                    "The plugin configuration is incoherent with the requests param : plugin id= <" + pPluginId
                            + ">- config id= <" + pConfigId + ">");
        }
        try {
            pluginConfiguration = pluginService.updatePluginConfiguration(pPluginConfiguration);
        } catch (PluginUtilsException e) {
            LOGGER.error("Cannot update the plugin configuration : <" + pConfigId + ">", e);
            throw new EntityNotFoundException(pConfigId.toString(), PluginConfiguration.class);
        }
        final Resource<PluginConfiguration> resource = new Resource<>(pluginConfiguration);

        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deletePluginConfiguration(@PathVariable("configId") String pPluginId,
            @PathVariable("pluginId") Long pConfigId) throws EntityNotFoundException {
        try {
            pluginService.deletePluginConfiguration(pConfigId);
        } catch (PluginUtilsException e) {
            LOGGER.error("Cannot delete the plugin configuration : <" + pConfigId + ">", e);
            throw new EntityNotFoundException(pConfigId.toString(), PluginConfiguration.class);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
