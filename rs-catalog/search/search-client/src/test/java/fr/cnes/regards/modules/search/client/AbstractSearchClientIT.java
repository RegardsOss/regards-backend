/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.client;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Set;

import org.assertj.core.util.Lists;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.search.dao.ISearchEngineConfRepository;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.legacy.LegacySearchEngine;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.EngineConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchEngine;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.ParameterConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.geo.GeoTimeExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.media.MediaExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.regards.RegardsExtension;
import fr.cnes.regards.modules.search.service.ISearchEngineConfigurationService;

/**
 * Abstract Integration Test for clients of the module.
 *
 * @author Xavier-Alexandre Brochard
 */
@TestPropertySource("classpath:test.properties")
public abstract class AbstractSearchClientIT<T> extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSearchClientIT.class);

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    /**
     * ElasticSearch repository
     */
    @Autowired
    private IEsRepository esRepository;

    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    @Autowired
    private ISearchEngineConfRepository engineRepo;

    @Autowired
    protected IPluginService pluginService;

    @Autowired
    protected ISearchEngineConfigurationService searchEngineService;

    protected T client;

    @Before
    public void setUp() throws ModuleException {
        client = FeignClientBuilder.build(new TokenClientProvider<>(getClazz(),
                "http://" + serverAddress + ":" + getPort(), feignSecurityManager));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        engineRepo.deleteAll();
        pluginConfRepo.deleteAll();

        // Init required index in the ElasticSearch repository
        if (!esRepository.indexExists(getDefaultTenant())) {
            esRepository.createIndex(getDefaultTenant());
        } else {
            esRepository.deleteAll(getDefaultTenant());
        }

        initPlugins();

        FeignSecurityManager.asSystem();
    }

    private void initPlugins() throws ModuleException {
        PluginConfiguration legacyConf = PluginUtils
                .getPluginConfiguration(PluginParametersFactory.build().getParameters(), LegacySearchEngine.class);
        legacyConf = pluginService.savePluginConfiguration(legacyConf);
        SearchEngineConfiguration seConf = new SearchEngineConfiguration();
        seConf.setLabel("Legacy conf for all datasets");
        seConf.setConfiguration(legacyConf);
        searchEngineService.createConf(seConf);

        GeoTimeExtension geoTime = new GeoTimeExtension();
        geoTime.setActivated(true);
        RegardsExtension regardsExt = new RegardsExtension();
        regardsExt.setActivated(true);
        MediaExtension mediaExt = new MediaExtension();
        mediaExt.setActivated(true);

        List<ParameterConfiguration> paramConfigurations = Lists.newArrayList();
        ParameterConfiguration planetParameter = new ParameterConfiguration();
        planetParameter.setAttributeModelJsonPath("properties.planet");
        planetParameter.setName("planet");
        planetParameter.setOptionsEnabled(true);
        planetParameter.setOptionsCardinality(10);
        paramConfigurations.add(planetParameter);

        ParameterConfiguration startTimeParameter = new ParameterConfiguration();
        startTimeParameter.setAttributeModelJsonPath("properties.TimePeriod.startDate");
        startTimeParameter.setName("start");
        startTimeParameter.setNamespace("time");
        paramConfigurations.add(startTimeParameter);
        ParameterConfiguration endTimeParameter = new ParameterConfiguration();
        endTimeParameter.setAttributeModelJsonPath("properties.TimePeriod.stopDate");
        endTimeParameter.setName("end");
        endTimeParameter.setNamespace("time");
        paramConfigurations.add(endTimeParameter);

        EngineConfiguration engineConfiguration = new EngineConfiguration();
        engineConfiguration.setAttribution("Plop");
        engineConfiguration.setSearchDescription("desc");
        engineConfiguration.setSearchTitle("search");
        engineConfiguration.setContact("regards@c-s.fr");
        engineConfiguration.setImage("http://plop/image.png");
        engineConfiguration.setEntityLastUpdateDatePropertyPath("TimePeriod.startDate");

        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(OpenSearchEngine.TIME_EXTENSION_PARAMETER, geoTime)
                .addParameter(OpenSearchEngine.REGARDS_EXTENSION_PARAMETER, regardsExt)
                .addParameter(OpenSearchEngine.MEDIA_EXTENSION_PARAMETER, mediaExt)
                .addParameter(OpenSearchEngine.PARAMETERS_CONFIGURATION, paramConfigurations)
                .addParameter(OpenSearchEngine.ENGINE_PARAMETERS, engineConfiguration).getParameters();
        PluginConfiguration opensearchConf = PluginUtils.getPluginConfiguration(parameters, OpenSearchEngine.class);
        PluginConfiguration openSearchPluginConf = pluginService.savePluginConfiguration(opensearchConf);
        SearchEngineConfiguration seConfOS = new SearchEngineConfiguration();
        seConfOS.setConfiguration(openSearchPluginConf);
        seConfOS.setLabel("Opensearch conf for all datasets");
        searchEngineService.createConf(seConfOS);

    }

    @After
    public void tearDown() {
        try {
            esRepository.deleteIndex(getDefaultTenant());
        } catch (IndexNotFoundException e) {
            // Who cares ?
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @SuppressWarnings("unchecked")
    protected Class<T> getClazz() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
