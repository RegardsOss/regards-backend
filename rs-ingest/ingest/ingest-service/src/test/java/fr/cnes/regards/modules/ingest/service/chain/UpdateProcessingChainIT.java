/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.chain;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoIT;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.plugin.AIPGenerationTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.ValidationTestPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

/**
 * Create and update a processing chain (without test transaction that can't work)
 *
 * @author Marc Sordi
 */
@RunWith(SpringRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingestu",
                                   "jwt.secret=123456789",
                                   "regards.workspace=target/workspace",
                                   "eureka.client.enabled=false" },
                    locations = { "classpath:application-test.properties" })
@ContextConfiguration(classes = { UpdateProcessingChainIT.IngestConfiguration.class })
public class UpdateProcessingChainIT extends AbstractDaoIT {

    private static final String CHAIN_NAME = "ipst_Chain1";

    @Autowired
    private IIngestProcessingChainService ingestProcessingService;

    @Autowired
    private IPluginService pluginService;

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules" })
    static class IngestConfiguration {

    }

    @Before
    public void cleanBefore() throws ModuleException {
        injectDefaultToken();
        if (ingestProcessingService.existsChain(CHAIN_NAME)) {
            ingestProcessingService.deleteChain(CHAIN_NAME);
        }
    }

    private IngestProcessingChain createBaseChain() throws ModuleException {
        IngestProcessingChain newChain = new IngestProcessingChain();
        newChain.setDescription("Ingest processing chain");
        newChain.setName(CHAIN_NAME);

        PluginConfiguration validation = PluginConfiguration.build(ValidationTestPlugin.class, null, Sets.newHashSet());
        validation.setIsActive(true);
        validation.setLabel("validationPlugin_ipst");
        newChain.setValidationPlugin(validation);

        PluginConfiguration generation = PluginConfiguration.build(AIPGenerationTestPlugin.class,
                                                                   null,
                                                                   Sets.newHashSet());
        generation.setIsActive(true);
        generation.setLabel("generationPlugin_ipst");
        newChain.setGenerationPlugin(generation);

        return ingestProcessingService.createNewChain(newChain);
    }

    @Test
    public void updateAndCleanProcessingChain() throws ModuleException {
        IngestProcessingChain chain = createBaseChain();

        // Register label
        String validationLabel = chain.getValidationPlugin().getLabel();
        String generationLabel = chain.getGenerationPlugin().getLabel();

        PluginConfiguration validation = PluginConfiguration.build(ValidationTestPlugin.class, null, Sets.newHashSet());
        validation.setIsActive(true);
        validation.setLabel("validationPlugin_ipst_new");
        chain.setValidationPlugin(validation);

        PluginConfiguration generation = PluginConfiguration.build(AIPGenerationTestPlugin.class,
                                                                   null,
                                                                   Sets.newHashSet());
        generation.setIsActive(true);
        generation.setLabel("generationPlugin_ipst_new");
        chain.setGenerationPlugin(generation);

        ingestProcessingService.updateChain(chain);

        Optional<PluginConfiguration> conf = pluginService.findPluginConfigurationByLabel(validationLabel);
        Assert.assertFalse(conf.isPresent());

        conf = pluginService.findPluginConfigurationByLabel(generationLabel);
        Assert.assertFalse(conf.isPresent());
    }
}
