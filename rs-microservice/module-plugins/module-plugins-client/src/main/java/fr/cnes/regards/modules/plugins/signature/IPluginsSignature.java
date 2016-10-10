/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.signature;

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;

/**
 * TODO descriptipn
 * 
 * @author cmertz
 *
 */
public interface IPluginsSignature {

    @RequestMapping(value = "/plugins", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<PluginMetaData>>> getPlugins();

    @RequestMapping(value = "/plugins/{pluginId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<PluginParameter>>> getPluginParameter();

    @RequestMapping(value = "/plugins/{pluginId}/config", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<PluginConfiguration>>> getPluginConfigurations();

    @RequestMapping(value = "/plugins/{pluginId}/config", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<PluginConfiguration>> savePluginConfiguration(
            @Valid @RequestBody PluginConfiguration pPluginConfiguration) throws AlreadyExistingException;
    //
    // @RequestMapping(value = "/plugins/{pluginId}/config/{configId}", method = RequestMethod.GET, produces =
    // MediaType.APPLICATION_JSON_VALUE)
    // @ResponseBody
    // HttpEntity<List<Resource<PluginConfiguration>>> getPluginConfiguration();
    //
    // @RequestMapping(value = "/plugins/{pluginId}/config/{configId}", method = RequestMethod.PUT, consumes =
    // MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    // @ResponseBody
    // HttpEntity<Resource<PluginConfiguration>> getPluginConfiguration();
    //
    // @RequestMapping(value = "/plugins/{pluginId}/config/{configId}", method = RequestMethod.DELETE, produces =
    // MediaType.APPLICATION_JSON_VALUE)
    // @ResponseBody
    // HttpEntity<List<Resource<PluginConfiguration>>> getPluginConfiguration();

    @RequestMapping(value = "/plugintypes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<String>>> getPluginTypes();

}
