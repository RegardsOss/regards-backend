/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.rest;

import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.signature.IPluginsSignature;


/**
 * REST module controller
 * 
 * TODO Description
  * 
* @author cmertz
 *
 */
@RestController
@ModuleInfo(name="plugins", version="1.0-SNAPSHOT", author="REGARDS", legalOwner="CS", documentation="http://test")
@RequestMapping("/plugins")
public class PluginController implements IPluginsSignature {

    @Override
    public HttpEntity<List<Resource<PluginMetaData>>> getPlugins() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<List<Resource<PluginParameter>>> getPluginParameter() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<List<Resource<PluginConfiguration>>> getPluginConfigurations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Resource<PluginConfiguration>> savePluginConfiguration(PluginConfiguration pPluginConfiguration)
            throws AlreadyExistingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<List<Resource<String>>> getPluginTypes() {
        // TODO Auto-generated method stub
        return null;
    }


}
