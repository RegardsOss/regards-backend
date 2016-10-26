/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.rest.AbstractController;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.plugins.signature.IPluginsSignature;

/**
 * Controller for REST Access to Puugin entities
 *
 * @author cmertz
 *
 */
@RestController
@ModuleInfo(name = "plugins", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
public class PluginController extends AbstractController implements IPluginsSignature {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginController.class);

    /**
     * Business service for Project entities. Autowired.
     */
    private final IPluginService pluginService;

    public PluginController(final IPluginService pPluginService) {
        super();
        pluginService = pPluginService;
    }

    @Override
    @ResourceAccess(description = "Get all plugins", name = "plugin")
    public ResponseEntity<List<Resource<PluginMetaData>>> getPlugins() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @ResourceAccess(description = "Get all plugins", name = "plugin")
    public ResponseEntity<List<Resource<PluginParameter>>> getPluginParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<List<Resource<PluginConfiguration>>> getPluginConfigurations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Resource<PluginConfiguration>> savePluginConfiguration(
            PluginConfiguration pPluginConfiguration) throws AlreadyExistingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Resource<PluginConfiguration>> getPluginConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Resource<PluginConfiguration>> updatePluginConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Void> deletePluginConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<List<Resource<String>>> getPluginTypes() {
        // TODO Auto-generated method stub
        return null;
    }

}
