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
package fr.cnes.regards.modules.ingest.service.chain;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.TestConfiguration;
import fr.cnes.regards.modules.ingest.service.plugin.AIPGenerationTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.ValidationTestPlugin;

@ActiveProfiles({ "disable-scheduled-ingest" })
@TestPropertySource(locations = "classpath:test.properties")
@ContextConfiguration(classes = { TestConfiguration.class })
@RegardsTransactional
public class IngestProcessingServiceTest extends AbstractRegardsServiceTransactionalIT {

    @Autowired
    private IIngestProcessingService ingestProcessingService;

    @Test
    public void createProcessingChain() throws ModuleException {

        PluginConfiguration conf = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), AIPGenerationTestPlugin.class, Lists.newArrayList());
        conf.setIsActive(true);
        conf.setLabel("generationPlugin_ipst");
        conf.setPriorityOrder(0);
        conf.setVersion("1.0");

        PluginConfiguration confVal = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), ValidationTestPlugin.class, Lists.newArrayList());
        conf.setIsActive(true);
        conf.setLabel("validationPlugin_ipst");
        conf.setPriorityOrder(0);
        conf.setVersion("1.0");

        IngestProcessingChain newChain = new IngestProcessingChain();
        newChain.setDescription("Ingest processing chain");
        newChain.setName("ipst_Chain1");
        newChain.setGenerationPlugin(conf);
        newChain.setValidationPlugin(confVal);

        newChain = ingestProcessingService.createNewChain(newChain);
        Assert.assertNotNull(newChain.getId());

        // Create a second chain with same plugin configurations
        IngestProcessingChain secondChain = new IngestProcessingChain();
        secondChain.setDescription("Ingest processing chain");
        secondChain.setName("ipst_Chain2");
        secondChain.setGenerationPlugin(newChain.getGenerationPlugin());
        secondChain.setValidationPlugin(newChain.getValidationPlugin());

        secondChain = ingestProcessingService.createNewChain(secondChain);
        Assert.assertNotNull(secondChain.getId());

        // Create a third chain with same plugin configurations but by editing them
        PluginConfiguration editedConf = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), AIPGenerationTestPlugin.class, Lists.newArrayList());
        editedConf.setIsActive(true);
        editedConf.setLabel("generationPlugin_ipst");
        editedConf.setPriorityOrder(0);
        editedConf.setVersion("2.0");
        editedConf.setId(newChain.getGenerationPlugin().getId());
        IngestProcessingChain thirdChain = new IngestProcessingChain();
        thirdChain.setDescription("Ingest processing chain");
        thirdChain.setName("ipst_Chain3");
        thirdChain.setGenerationPlugin(editedConf);
        thirdChain.setValidationPlugin(newChain.getValidationPlugin());

        thirdChain = ingestProcessingService.createNewChain(thirdChain);
        Assert.assertNotNull(thirdChain.getId());
        Assert.assertEquals("2.0", thirdChain.getGenerationPlugin().getVersion());

        // Retrieve the third chain
        Page<IngestProcessingChain> chains = ingestProcessingService.searchChains("ipst_", new PageRequest(0, 10));
        Assert.assertEquals(3, chains.getTotalElements());
        // Check that the plugin modification is set for all chain that use the same conf
        for (IngestProcessingChain chain : chains.getContent()) {
            Assert.assertEquals("2.0", chain.getGenerationPlugin().getVersion());
        }

        // Test update chain
        thirdChain = ingestProcessingService.updateChain(thirdChain);
        Assert.assertNotNull(thirdChain);

        // Test delete chain
        ingestProcessingService.deleteChain(thirdChain.getName());
        try {
            ingestProcessingService.getChain(thirdChain.getName());
            Assert.fail("Entity should be deleted");
        } catch (EntityNotFoundException e) {
            // Nothing to do
        }
    }

}
