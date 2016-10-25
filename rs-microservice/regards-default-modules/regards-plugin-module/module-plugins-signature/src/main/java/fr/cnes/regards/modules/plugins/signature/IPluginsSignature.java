/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.signature;

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;

/**
 * Signature interface for Plugins module.
 * 
 * @author cmertz
 *
 */
public interface IPluginsSignature {

    /**
     * Get all the plugins identifies by the annotation {@link Plugin}.
     * 
     * @return a list of {@link PluginMetaData}
     */
    @RequestMapping(value = "/plugins", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<PluginMetaData>>> getPlugins();

    /**
     * Get all the metadata of a specified plugin.
     * 
     * @return
     */
    @RequestMapping(value = "/plugins/{pluginId}", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<PluginParameter>>> getPluginParameters();

    /**
     * Get all the {@link PluginConfiguration} of a specified plugin.
     * 
     * @return a list of {@link PluginConfiguration}
     */
    @RequestMapping(value = "/plugins/{pluginId}/config", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<PluginConfiguration>>> getPluginConfigurations();

    /**
     * Create a new {@link PluginConfiguration}.
     * 
     * @param pPluginConfiguration
     * @return the {@link PluginConfiguration] created.
     * @throws AlreadyExistingException
     */
    @RequestMapping(value = "/plugins/{pluginId}/config", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<PluginConfiguration>> savePluginConfiguration(
            @Valid @RequestBody PluginConfiguration pPluginConfiguration) throws AlreadyExistingException;

    /**
     * Get the {@link PluginConfiguration} of a specified plugin.
     * 
     * @return the {@link PluginConfiguration} of the plugin.
     */
    @RequestMapping(value = "/plugins/{pluginId}/config/{configId}", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<PluginConfiguration>> getPluginConfiguration();

    /**
     * Update a {@link PluginConfiguration} of a specified plugin.
     * 
     * @return the {@link PluginConfiguration} of the plugin.
     */
    @RequestMapping(value = "/plugins/{pluginId}/config/{configId}", method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<PluginConfiguration>> updatePluginConfiguration();

    /**
     * Delete a {@link PluginConfiguration}.
     * 
     * @return void
     */
    @RequestMapping(value = "/plugins/{pluginId}/config/{configId}", method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Void> deletePluginConfiguration();

    /**
     * Get the interface identified with the annotation {@link PluginInterface}.
     * 
     * @return a list of interface annotated with {@link PluginInterface}.
     */
    @RequestMapping(value = "/plugintypes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<String>>> getPluginTypes();

}
