/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.plugins.client.rest;

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

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@RequestMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IPluginClient {

    /**
     * REST mapping resource : /plugins
     */
    String PLUGINS = "/plugins";

    /**
     * REST mapping resource : /plugintypes
     */
    String PLUGIN_TYPES = "/plugintypes";

    /**
     * REST mapping resource : /plugins/{pluginId}
     */
    String PLUGINS_PLUGINID = PLUGINS + "/{pluginId}";

    /**
     * REST mapping resource : /plugins/{pluginId}/config
     */
    String PLUGINS_PLUGINID_CONFIGS = PLUGINS_PLUGINID + "/config";

    /**
     * REST mapping resource : /plugins/configs
     */
    String PLUGINS_CONFIGS = PLUGINS + "/configs";

    /**
     * REST mapping resource : /plugins/{pluginId}/config/{configId}
     */
    String PLUGINS_PLUGINID_CONFIGID = PLUGINS_PLUGINID_CONFIGS + "/{configId}";

    /**
     * REST mapping resource : /plugins/configs/{configId}
     */
    String PLUGINS_CONFIGID = PLUGINS_CONFIGS + "/{configId}";

    /**
     * Get all the plugins identifies by the annotation {@link Plugin}.
     * @param pPluginType a type of plugin
     * @return a {@link List} of {@link PluginMetaData}
     */
    @RequestMapping(value = PLUGINS, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<PluginMetaData>>> getPlugins(
            @RequestParam(value = "pluginType", required = false) final String pPluginType);

    /**
     * Get the interface identified with the annotation {@link PluginInterface}.
     * @return a {@link List} of interface annotated with {@link PluginInterface}
     */
    @RequestMapping(value = PLUGIN_TYPES, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<String>>> getPluginTypes();

    /**
     * Get all the metadata of a specified plugin.
     * @param pPluginId a plugin identifier
     * @return a {@link PluginMetaData}
     */
    @RequestMapping(value = PLUGINS_PLUGINID, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<PluginMetaData>> getPluginMetaDataById(@PathVariable("pluginId") final String pPluginId);

    /**
     * Get all the {@link PluginConfiguration} of a specified plugin.
     * @param pPluginId a plugin identifier
     * @return a {@link List} of {@link PluginConfiguration}
     */
    @RequestMapping(value = PLUGINS_PLUGINID_CONFIGS, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<PluginConfiguration>>> getPluginConfigurations(
            @PathVariable("pluginId") final String pPluginId);

    /**
     * Get all the {@link PluginConfiguration} for a specific plugin type.</br>
     * If any specific plugin type is defined, get all the {@link PluginConfiguration}.
     * @param pPluginType an interface name, that implements {@link PluginInterface}.<br>
     * This parameter is optional.
     * @return a {@link List} of {@link PluginConfiguration}
     */
    @RequestMapping(value = PLUGINS_CONFIGS, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<PluginConfiguration>>> getPluginConfigurationsByType(
            @RequestParam(value = "pluginType", required = false) final String pPluginType);

    /**
     * Create a new {@link PluginConfiguration}.
     * @param pPluginConfiguration a {@link PluginConfiguration}
     * @return the created {@link PluginConfiguration}
     */
    @RequestMapping(value = PLUGINS_PLUGINID_CONFIGS, method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<PluginConfiguration>> savePluginConfiguration(
            @Valid @RequestBody final PluginConfiguration pPluginConfiguration);

    /**
     * Get the {@link PluginConfiguration} of a specified plugin.
     * @param pPluginId a plugin identifier
     * @param pConfigId a plugin configuration identifier
     * @return the {@link PluginConfiguration} of the plugin
     */
    @RequestMapping(value = PLUGINS_PLUGINID_CONFIGID, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<PluginConfiguration>> getPluginConfiguration(
            @PathVariable("pluginId") final String pPluginId, @PathVariable("configId") final Long pConfigId);

    /**
     * Get the {@link PluginConfiguration} of a specified plugin.
     * @param pConfigId a plugin configuration identifier
     * @return the {@link PluginConfiguration} of the plugin
     */
    @RequestMapping(value = PLUGINS_CONFIGID, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<PluginConfiguration>> getPluginConfigurationDirectAccess(
            @PathVariable("configId") final Long pConfigId);

    /**
     * Update a {@link PluginConfiguration} of a specified plugin.
     * @param pPluginId a plugin identifier
     * @param pConfigId a plugin configuration identifier
     * @param pPluginConfiguration a {@link PluginConfiguration}
     * @return the {@link PluginConfiguration} of the plugin.
     */
    @RequestMapping(value = PLUGINS_PLUGINID_CONFIGID, method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<PluginConfiguration>> updatePluginConfiguration(
            @PathVariable("pluginId") final String pPluginId, @PathVariable("configId") final Long pConfigId,
            @Valid @RequestBody final PluginConfiguration pPluginConfiguration);

    /**
     * Delete a {@link PluginConfiguration}.
     * @param pPluginId a plugin identifier
     * @param pConfigId a plugin configuration identifier
     * @return void
     */
    @RequestMapping(value = PLUGINS_PLUGINID_CONFIGID, method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Void> deletePluginConfiguration(@PathVariable("pluginId") final String pPluginId,
            @PathVariable("configId") final Long pConfigId);
}
