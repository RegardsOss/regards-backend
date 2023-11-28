/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.search.domain.plugin.IEntityLinkBuilder;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineConfiguration;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.service.engine.ISearchEngineDispatcher;
import fr.cnes.regards.modules.search.service.engine.SearchEngineDispatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;
import java.util.UUID;

/**
 * Test the {@link SearchEngineDispatcher}
 *
 * @author Sébastien Binda
 */
@TestPropertySource(locations = { "classpath:test.properties" },
                    properties = { "regards.tenant=opensearch",
                                   "spring.jpa.properties.hibernate.default_schema=opensearch" })
@MultitenantTransactional
public class SearchEngineDispatcherIT extends AbstractRegardsTransactionalIT {

    @Autowired
    protected IPluginService pluginService;

    @Autowired
    protected ISearchEngineConfigurationService searchEngineService;

    @Autowired
    protected ISearchEngineDispatcher dispatcher;

    @Autowired
    protected IEntityLinkBuilder linkBuilder;

    private static final String DATASET1_URN = "URN:AIP:"
                                               + EntityType.DATASET.toString()
                                               + ":PROJECT:"
                                               + UUID.randomUUID()
                                               + ":V1";

    private static final String DATASET2_URN = "URN:AIP:"
                                               + EntityType.DATASET.toString()
                                               + ":PROJECT:"
                                               + UUID.randomUUID()
                                               + ":V2";

    private static final String DATASET3_URN = "URN:AIP:"
                                               + EntityType.DATASET.toString()
                                               + ":PROJECT:"
                                               + UUID.randomUUID()
                                               + ":V3";

    @Before
    public void init() throws ModuleException {

        // First conf for engine associated to dataset1
        Set<IPluginParam> pluginParams = IPluginParam.set(IPluginParam.build(SearchEngineTest.DATASET_PARAM,
                                                                             DATASET1_URN));
        PluginConfiguration engineConf = PluginConfiguration.build(SearchEngineTest.class, null, pluginParams);
        engineConf = pluginService.savePluginConfiguration(engineConf);
        SearchEngineConfiguration seConf = new SearchEngineConfiguration();
        seConf.setLabel("Engine for dataset1");
        seConf.setConfiguration(engineConf);
        seConf.setDatasetUrn(DATASET1_URN);
        searchEngineService.createConf(seConf);

        // Second conf for engine associated to dataset2
        Set<IPluginParam> pluginParams2 = IPluginParam.set(IPluginParam.build(SearchEngineTest.DATASET_PARAM,
                                                                              DATASET2_URN));
        PluginConfiguration engineConf2 = PluginConfiguration.build(SearchEngineTest.class, null, pluginParams2);
        engineConf2 = pluginService.savePluginConfiguration(engineConf2);
        seConf = new SearchEngineConfiguration();
        seConf.setLabel("Engine for dataset2");
        seConf.setConfiguration(engineConf2);
        seConf.setDatasetUrn(DATASET2_URN);
        searchEngineService.createConf(seConf);

        // Third conf for engine associated to no dataset
        Set<IPluginParam> pluginParams3 = IPluginParam.set();
        PluginConfiguration engineConf3 = PluginConfiguration.build(SearchEngineTest.class, null, pluginParams3);
        engineConf3 = pluginService.savePluginConfiguration(engineConf3);
        seConf = new SearchEngineConfiguration();
        seConf.setLabel("Engine for all datasets");
        seConf.setConfiguration(engineConf3);
        searchEngineService.createConf(seConf);

    }

    @Test
    public void testDispatchEngine() throws ModuleException {

        SearchContext context = new SearchContext();
        context.setSearchType(SearchType.ALL);
        HttpHeaders headers = new HttpHeaders();
        context.setHeaders(headers);

        // Check that the plugin conf associated to DATASET1 is dispatch when context contains dataset1
        context.setDatasetUrn(UniformResourceName.fromString(DATASET1_URN));
        context.setEngineType(SearchEngineTest.ENGINE_ID);
        ResponseEntity<Object> response = dispatcher.dispatchRequest(context, linkBuilder);
        Assert.assertEquals(
            "SearchEngine Plugin conf associated to DATASET1 should be dispatch when context contains dataset1",
            DATASET1_URN,
            response.getBody());

        // Check that the plugin conf associated to DATASET2 is dispatch when context contains dataset2
        context.setDatasetUrn(UniformResourceName.fromString(DATASET2_URN));
        context.setEngineType(SearchEngineTest.ENGINE_ID);
        response = dispatcher.dispatchRequest(context, linkBuilder);
        Assert.assertEquals(
            "SearchEngine Plugin conf associated to DATASET2 should be dispatch when context contains dataset2",
            DATASET2_URN,
            response.getBody());

        // Check that the plugin conf associated to no dataset is dispatch when context contains dataset3
        context.setDatasetUrn(UniformResourceName.fromString(DATASET3_URN));
        context.setEngineType(SearchEngineTest.ENGINE_ID);
        response = dispatcher.dispatchRequest(context, linkBuilder);
        Assert.assertEquals(
            "SearchEngine Plugin conf associated to no dataset should be dispatch when context contains dataset3",
            null,
            response.getBody());

        // Check that the plugin conf associated to no dataset is dispatch when context does not contains any dataset
        context.setDatasetUrn(null);
        context.setEngineType(SearchEngineTest.ENGINE_ID);
        response = dispatcher.dispatchRequest(context, linkBuilder);
        Assert.assertEquals(
            "SearchEngine Plugin conf associated to no dataset should be dispatch when context does not contains any dataset",
            null,
            response.getBody());

        // Check that no search engine is found for an unkown engine type
        context.setDatasetUrn(null);
        context.setEngineType("unknown");
        try {
            dispatcher.dispatchRequest(context, linkBuilder);
            Assert.fail("No SearchEngine should be found for unknow engine type");
        } catch (EntityNotFoundException e) {
            // Nothinf to do. An exception should be thrown here
        }

    }

}
