/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.signature;

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;
import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;

/**
 * Signature interface for Plugins module.
 * 
 * @author Christophe Mertz
 *
 */
public interface IPluginsSignature {

    /**
     * Get all the plugins identifies by the annotation {@link Plugin}.
     * 
     * @param pPluginType
     *            a type of plugin
     * 
     * @return a list of {@link PluginMetaData}
     * 
     * @throws EntityException
     *             if problem occurs
     */
    @RequestMapping(value = "/plugins", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<PluginMetaData>>> getPlugins(
            @RequestParam(value = "pluginType", required = false) final String pPluginType) throws EntityException;

    /**
     * Get the interface identified with the annotation {@link PluginInterface}.
     * 
     * @return a list of interface annotated with {@link PluginInterface}.
     */
    @RequestMapping(value = "/plugintypes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<String>>> getPluginTypes();

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
    ResponseEntity<Resource<PluginMetaData>> getPluginMetaDataById(@PathVariable("pluginId") String pPluginId);

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
    ResponseEntity<List<Resource<PluginConfiguration>>> getPluginConfigurations(
            @PathVariable("pluginId") String pPluginId);

    /**
     * Create a new {@link PluginConfiguration}.
     * 
     * @param pPluginConfiguration
     *            a {@link PluginConfiguration}
     *            
     * @return the {@link PluginConfiguration] created
     * 
     * @throws InvalidValueException
     *             if problem occurs
     */
    @RequestMapping(value = "/plugins/{pluginId}/config", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<PluginConfiguration>> savePluginConfiguration(
            @Valid @RequestBody PluginConfiguration pPluginConfiguration) throws InvalidValueException;

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
    ResponseEntity<Resource<PluginConfiguration>> getPluginConfiguration(@PathVariable("pluginId") String pPluginId,
            @PathVariable("configId") Long pConfigId) throws EntityNotFoundException;

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
     * @throws InvalidValueException
     *             the {@link PluginConfiguration} is incoherent with the path parameter
     *             
     */
    @RequestMapping(value = "/plugins/{pluginId}/config/{configId}", method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<PluginConfiguration>> updatePluginConfiguration(@PathVariable("pluginId") String pPluginId,
            @PathVariable("configId") Long pConfigId, @Valid @RequestBody PluginConfiguration pPluginConfiguration)
            throws EntityNotFoundException, InvalidValueException;

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
    ResponseEntity<Void> deletePluginConfiguration(@PathVariable("pluginId") String pPluginId,
            @PathVariable("configId") Long pConfigId) throws EntityNotFoundException;

}
