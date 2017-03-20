/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.ArrayList;
import java.util.StringJoiner;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.search.domain.IService;
import fr.cnes.regards.modules.search.rest.plugin.TestService;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class ServicesControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOG = LoggerFactory.getLogger(ServicesControllerIT.class);

    private final ArrayList<ResultMatcher> expectations = new ArrayList<>();

    @Autowired
    private IPluginService pluginService;

    private PluginConfiguration conf;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Before
    public void init() throws ModuleException {
        PluginParameter parameter = new PluginParameter("para", "never used");
        parameter.setIsDynamic(true);
        PluginMetaData metaData = new PluginMetaData();
        metaData.setPluginId("tata");
        metaData.setAuthor("toto");
        metaData.setDescription("titi");
        metaData.setVersion("tutu");
        metaData.setInterfaceName(IService.class.getName());
        metaData.setPluginClassName(TestService.class.getName());
        conf = new PluginConfiguration(metaData, "testConf");
        conf.setParameters(Lists.newArrayList(parameter));
        pluginService.addPluginPackage(TestService.class.getPackage().getName());
        pluginService.savePluginConfiguration(conf);
    }

    @Test
    public void testRetrieveServices() {
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(ServicesController.PATH_SERVICES, expectations, "there should not be any error");
    }

    @Test
    public void testApplyService() {
        StringJoiner sj = new StringJoiner("&", "?q=truc", "");
        sj.add("para=" + TestService.EXPECTED_VALUE);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(ServicesController.PATH_SERVICE_NAME, expectations, "there should not be any error",
                          conf.getLabel());
    }

}
