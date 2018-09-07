package fr.cnes.regards.framework.modules.plugins.service.it;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.ISamplePlugin;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;

/**
 * Integration tests on sensitive plugin parameter handling
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=plugins")
public class PluginServiceEncryptionIT extends AbstractRegardsServiceTransactionalIT {

    private static final java.lang.String PLUGIN_CONF_LABEL = "PluginServiceEncryptionIT";

    @Autowired
    private IEncryptionService encryptionService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IPluginConfigurationRepository pluginRepository;

    @Test
    public void testSaveSensitiveConf() throws EncryptionException, EntityInvalidException, EntityNotFoundException {
        PluginMetaData pluginMeta = PluginUtils.createPluginMetaData(SensitivePlugin.class);
        String paramValue = "Un petit test";
        String encryptedParamValue = encryptionService.encrypt(paramValue);
        List<PluginParameter> params = PluginParametersFactory.build()
                .addParameter(SensitivePlugin.MESSAGE_PLUGIN_PARAM, paramValue).getParameters();
        PluginConfiguration pluginConf = new PluginConfiguration(pluginMeta, PLUGIN_CONF_LABEL, params, 0);
        Long dbId = pluginService.savePluginConfiguration(pluginConf).getId();
        // now that it has been saved, lets check that parameter has been encrypted into DB
        PluginConfiguration dbPluginConf = pluginService.loadPluginConfiguration(dbId);
        // Thanks to gson normalization, some chars are escaped into the hex form
        Assert.assertEquals("Plugin parameter value in DB should be encrypted",
                            "\"zRqQBURkGfCTNF+JLFkY7A\\u003d\\u003d\"",
                            dbPluginConf.getParameterValue(SensitivePlugin.MESSAGE_PLUGIN_PARAM));
    }

    @Test
    public void testInstanciateSensitivePlugin() throws ModuleException {
        PluginMetaData pluginMeta = PluginUtils.createPluginMetaData(SensitivePlugin.class);
        String paramValue = "Un petit test";
        List<PluginParameter> params = PluginParametersFactory.build()
                .addParameter(SensitivePlugin.MESSAGE_PLUGIN_PARAM, paramValue).getParameters();
        PluginConfiguration pluginConf = new PluginConfiguration(pluginMeta, PLUGIN_CONF_LABEL, params, 0);
        Long dbId = pluginService.savePluginConfiguration(pluginConf).getId();
        SensitivePlugin sensitivePlg = pluginService.getPlugin(dbId);
        Assert.assertEquals("Once the plugin instantiated, the parameter should not be encrypted anymore",
                            paramValue,
                            sensitivePlg.echo(""));
    }

    @Test
    public void testUpdateSensitiveParamUnchanged() throws ModuleException {
        PluginMetaData pluginMeta = PluginUtils.createPluginMetaData(SensitivePlugin.class);
        String paramValue = "Un petit test";
        String encryptedParamValue = encryptionService.encrypt(paramValue);
        List<PluginParameter> params = PluginParametersFactory.build()
                .addParameter(SensitivePlugin.MESSAGE_PLUGIN_PARAM, paramValue).getParameters();
        PluginConfiguration pluginConf = new PluginConfiguration(pluginMeta, PLUGIN_CONF_LABEL, params, 0);
        PluginConfiguration savedPlgConf = pluginService.savePluginConfiguration(pluginConf);
        savedPlgConf.setIsActive(!savedPlgConf.isActive());
        PluginConfiguration updated = pluginService.updatePluginConfiguration(savedPlgConf);
        Assert.assertEquals("Plugin parameter value in DB should not have changed",
                            "\"zRqQBURkGfCTNF+JLFkY7A\\u003d\\u003d\"",
                            updated.getParameterValue(SensitivePlugin.MESSAGE_PLUGIN_PARAM));
    }

    @Test
    public void testUpdateSensitiveParam() throws ModuleException {
        PluginMetaData pluginMeta = PluginUtils.createPluginMetaData(SensitivePlugin.class);
        String paramValue = "Un petit test";
        List<PluginParameter> params = PluginParametersFactory.build()
                .addParameter(SensitivePlugin.MESSAGE_PLUGIN_PARAM, paramValue).getParameters();
        PluginConfiguration pluginConf = new PluginConfiguration(pluginMeta, PLUGIN_CONF_LABEL, params, 0);
        PluginConfiguration savedPlgConf = pluginService.savePluginConfiguration(pluginConf);
        String updatedParamValue = paramValue + "2";
        String encryptedParamValue = encryptionService.encrypt(updatedParamValue);
        savedPlgConf.getParameter(SensitivePlugin.MESSAGE_PLUGIN_PARAM).setValue(updatedParamValue);
        PluginConfiguration updated = pluginService.updatePluginConfiguration(savedPlgConf);
        Assert.assertEquals("Plugin parameter value in DB should have been updated and encrypted",
                            "\"[P4PXqn1DisXaK1g9X5koZ]A\\u003d\\u003d\"",
                            updated.getParameterValue(SensitivePlugin.MESSAGE_PLUGIN_PARAM));
    }

    @After
    public void cleanUp() {
        pluginRepository.deleteAll();
    }

}
