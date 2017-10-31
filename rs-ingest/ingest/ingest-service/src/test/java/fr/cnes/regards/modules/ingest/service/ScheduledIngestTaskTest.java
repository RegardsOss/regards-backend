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
package fr.cnes.regards.modules.ingest.service;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.service.plugin.DefaultSingleAIPGeneration;
import fr.cnes.regards.modules.ingest.service.plugin.DefaultSipValidation;

/**
 * Test class to check scheduled tasks to handle created SIP to be processed by processing chains.
 * @author Sébastien Binda
 */
@RunWith(SpringRunner.class)
@TestPropertySource(properties = { "regards.ingest.process.new.sips.delay:3000" },
        locations = "classpath:test.properties")
@ContextConfiguration(classes = { ScheduledIngestTaskTest.IngestConfiguration.class })
public class ScheduledIngestTaskTest extends AbstractDaoTest {

    @Autowired
    private IIngestProcessingChainRepository processingChainRepository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    @Value("${regards.ingest.process.new.sips.delay}")
    private String scheduledTasksDelay;

    public static final String DEFAULT_PROCESSING_CHAIN_TEST = "defaultProcessingChain";

    public static final String SIP_ID_TEST = "SIP_001";

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules" })
    static class IngestConfiguration {
    }

    @Before
    public void init() throws ModuleException {
        pluginConfRepo.deleteAll();
        sipRepository.deleteAll();
        initDefaultProcessingChain();
    }

    private void initDefaultProcessingChain() throws ModuleException {
        PluginMetaData defaultValidationPluginMeta = PluginUtils.createPluginMetaData(DefaultSipValidation.class);
        PluginConfiguration defaultValidationPlugin = new PluginConfiguration(defaultValidationPluginMeta,
                "defaultValidationPlugin");
        pluginService.savePluginConfiguration(defaultValidationPlugin);

        PluginMetaData defaultGenerationPluginMeta = PluginUtils.createPluginMetaData(DefaultSingleAIPGeneration.class);
        PluginConfiguration defaultGenerationPlugin = new PluginConfiguration(defaultGenerationPluginMeta,
                "defaultGenerationPlugin");
        pluginService.savePluginConfiguration(defaultGenerationPlugin);

        IngestProcessingChain defaultChain = new IngestProcessingChain(DEFAULT_PROCESSING_CHAIN_TEST,
                "Default Ingestion processing chain", defaultValidationPlugin, defaultGenerationPlugin);
        processingChainRepository.save(defaultChain);
    }

    @Requirement("REGARDS_DSL_ING_PRO_120")
    @Purpose("Manage scheduled ingestion tasks for all CREATED SIP")
    @Test
    public void scheduleIngestJobTest() throws InterruptedException, ModuleException {

        // 1. Add a new SIP with CREATED Status.
        SIPCollectionBuilder colBuilder = new SIPCollectionBuilder(DEFAULT_PROCESSING_CHAIN_TEST, "sessionId");
        SIPCollection collection = colBuilder.build();
        SIPBuilder builder = new SIPBuilder(SIP_ID_TEST);
        collection.add(builder.build());
        Collection<SIPEntity> results = ingestService.ingest(collection);
        Long sipIdTest = results.stream().findFirst().get().getId();
        SIPEntity sip = sipRepository.findOne(sipIdTest);
        Assert.assertTrue("Error creating new SIP", SIPState.CREATED.equals(sip.getState()));

        // 2. Wait for scheduled task to be run and finished
        Thread.sleep(Integer.parseInt(scheduledTasksDelay) + 1000);

        // 3. Check that the SIP has been successully handled
        sip = sipRepository.findOne(sipIdTest);
        Assert.assertTrue("SIP should have been handled by the scheduled task.",
                          SIPState.AIP_CREATED.equals(sip.getState()));
    }

}
