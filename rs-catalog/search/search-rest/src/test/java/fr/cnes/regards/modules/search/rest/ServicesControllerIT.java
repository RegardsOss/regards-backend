/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.search.plugin.IService;
import fr.cnes.regards.modules.search.plugin.SampleServicePlugin;
import fr.cnes.regards.modules.search.rest.plugin.TestService;
import fr.cnes.regards.modules.search.service.link.ILinkPluginsDatasetsService;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@TestPropertySource(locations = "classpath:test.properties")
@ContextConfiguration(classes = { CatalogITConfiguration.class })
@MultitenantTransactional
public class ServicesControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOG = LoggerFactory.getLogger(ServicesControllerIT.class);

    private static final String DATA_SET_NAME = "test";

    private ArrayList<ResultMatcher> expectations;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ILinkPluginsDatasetsService linkService;

    private PluginConfiguration conf;

    private PluginConfiguration samplePlgConf;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Before
    public void init() throws ModuleException {
        expectations = new ArrayList<>();
        final PluginParameter parameter = new PluginParameter("para", "never used");
        parameter.setIsDynamic(true);
        final PluginMetaData metaData = new PluginMetaData();
        metaData.setPluginId("tata");
        metaData.setAuthor("toto");
        metaData.setDescription("titi");
        metaData.setVersion("tutu");
        metaData.getInterfaceNames().add(IService.class.getName());
        metaData.setPluginClassName(TestService.class.getName());
        conf = new PluginConfiguration(metaData, "testConf");
        conf.setParameters(Lists.newArrayList(parameter));
        pluginService.addPluginPackage(IService.class.getPackage().getName());
        pluginService.addPluginPackage(TestService.class.getPackage().getName());
        pluginService.addPluginPackage(SampleServicePlugin.class.getPackage().getName());
        pluginService.savePluginConfiguration(conf);

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameterDynamic(SampleServicePlugin.ACTIVE, "false")
                .addParameterDynamic(SampleServicePlugin.COEFF, "0")
                .addParameterDynamic(SampleServicePlugin.SUFFIXE, "Hello Toulouse",
                                     Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h"))
                .getParameters();
        samplePlgConf = new PluginConfiguration(
                PluginUtils.createPluginMetaData(SampleServicePlugin.class,
                                                 Arrays.asList(SampleServicePlugin.class.getPackage().getName())),
                "SampleServicePlugin", parameters);
        pluginService.savePluginConfiguration(samplePlgConf);

        linkService.updateLink(DATA_SET_NAME,
                               new LinkPluginsDatasets(DATA_SET_NAME, Sets.newHashSet(conf, samplePlgConf)));
    }

    @Test
    public void testRetrieveServicesQuery() {
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$").isArray());
        expectations.add(MockMvcResultMatchers.jsonPath("$").isNotEmpty());
        performDefaultGet(ServicesController.PATH_SERVICES + "?service_scope=QUERY", expectations,
                          "there should not be any error", DATA_SET_NAME);
    }

    @Test
    public void testRetrieveServicesMany() {
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$").isArray());
        expectations.add(MockMvcResultMatchers.jsonPath("$").isEmpty());
        performDefaultGet(ServicesController.PATH_SERVICES + "?service_scope=MANY", expectations,
                          "there should not be any error", DATA_SET_NAME);
    }

    @Test
    public void testRetrieveServicesOne() {
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$").isArray());
        expectations.add(MockMvcResultMatchers.jsonPath("$").isNotEmpty());
        performDefaultGet(ServicesController.PATH_SERVICES + "?service_scope=ONE", expectations,
                          "there should not be any error", DATA_SET_NAME);
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_ARC_030")
    @Requirement("REGARDS_DSL_DAM_ARC_010")
    @Purpose("System has a joinpoint \"Service\" that allows to apply treatment on a dataset, or one of its subset. Those treatments are applied to informations contained into the catalog. A plugin \"Service\" can have as parameters: parameters defined at configuration by an administrator, parameters dynamicly defined at each request, parameters to select objects from a dataset.")
    public void testApplyService() {
        final StringJoiner sj = new StringJoiner("&", "?", "");
        sj.add("q=truc");
        sj.add("para=" + TestService.EXPECTED_VALUE);
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$").isArray());
        expectations.add(MockMvcResultMatchers.jsonPath("$").isNotEmpty());
        performDefaultGet(ServicesController.PATH_SERVICES + ServicesController.PATH_SERVICE_NAME + sj.toString(),
                          expectations, "there should not be any error", DATA_SET_NAME, conf.getLabel());
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_310")
    @Purpose("System allows to set a dynamic plugin parameter in the HTPP request")
    public void testApplyServiceSetSpecificParamValue() {
        final StringJoiner sj = new StringJoiner("&", "?", "");
        sj.add("q=truc");
        sj.add("para=HelloWorld");
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$").isEmpty());
        performDefaultGet(ServicesController.PATH_SERVICES + ServicesController.PATH_SERVICE_NAME + sj.toString(),
                          expectations, "there should not be any error", DATA_SET_NAME, conf.getLabel());
    }

    @Test
    public void testSampleServiceNotActive() {
        final StringJoiner sj = new StringJoiner("&", "?", "");
        sj.add(SampleServicePlugin.COEFF + "=-1");
        sj.add(SampleServicePlugin.SUFFIXE + "=b");

        expectations.add(MockMvcResultMatchers.status().isNoContent());
        performDefaultGet(ServicesController.PATH_SERVICES + ServicesController.PATH_SERVICE_NAME + sj.toString(),
                          expectations, "there should not be any error", DATA_SET_NAME, samplePlgConf.getLabel());
    }

    @Test
    public void testSampleServiceActive() {
        final StringJoiner sj = new StringJoiner("&", "?", "");
        sj.add(SampleServicePlugin.ACTIVE + "=true");
        sj.add(SampleServicePlugin.COEFF + "=100");
        sj.add(SampleServicePlugin.SUFFIXE + "=h");

        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(ServicesController.PATH_SERVICES + ServicesController.PATH_SERVICE_NAME + sj.toString(),
                          expectations, "there should not be any error", DATA_SET_NAME, samplePlgConf.getLabel());
    }

    @Test
    public void testSampleServiceBadDynamicParameterValue() {
        final StringJoiner sj = new StringJoiner("&", "?", "");
        sj.add(SampleServicePlugin.ACTIVE + "=true");
        sj.add(SampleServicePlugin.COEFF + "=0");
        sj.add(SampleServicePlugin.SUFFIXE + "=z");

        expectations.add(MockMvcResultMatchers.status().isInternalServerError());
        performDefaultGet(ServicesController.PATH_SERVICES + ServicesController.PATH_SERVICE_NAME + sj.toString(),
                          expectations, "there should not be any error", DATA_SET_NAME, samplePlgConf.getLabel());
    }

}
