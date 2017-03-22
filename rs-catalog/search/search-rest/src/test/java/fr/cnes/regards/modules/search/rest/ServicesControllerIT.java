/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.ArrayList;
import java.util.StringJoiner;

import javax.transaction.Transactional;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.search.domain.IService;
import fr.cnes.regards.modules.search.rest.plugin.TestService;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TestPropertySource(locations = "classpath:test.properties")
@Transactional
public class ServicesControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOG = LoggerFactory.getLogger(ServicesControllerIT.class);

    private ArrayList<ResultMatcher> expectations;

    @Autowired
    private IPluginService pluginService;

    private PluginConfiguration conf;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Before
    public void init() throws ModuleException {
        expectations = new ArrayList<>();
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
        pluginService.addPluginPackage(IService.class.getPackage().getName());
        pluginService.savePluginConfiguration(conf);
    }

    @Test
    public void testRetrieveServices() {
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(ServicesController.PATH_SERVICES, expectations, "there should not be any error");
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_ARC_030")
    @Requirement("REGARDS_DSL_DAM_ARC_010")
    @Purpose("System has a joinpoint \"Service\" allow to apply treatment on a dataset, or one of its subset. Those treatments are applied to informations contained into the catalog. A plugin \"Service\" can have as parameters: parameters defined at configuration by an administrator, parameters dynamicly defined at each request, parameters to select objects from a dataset.")
    public void testApplyService() {
        StringJoiner sj = new StringJoiner("&", "?", "");
        sj.add("q=truc");
        sj.add("para=" + TestService.EXPECTED_VALUE);
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$").isArray());
        expectations.add(MockMvcResultMatchers.jsonPath("$").isNotEmpty());
        performDefaultGet(ServicesController.PATH_SERVICES + ServicesController.PATH_SERVICE_NAME + sj.toString(),
                          expectations, "there should not be any error", conf.getLabel());
    }

}
