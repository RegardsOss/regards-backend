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
package fr.cnes.regards.modules.ingest.service.chain;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.ingest.service.plugin.AIPGenerationTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.ValidationTestPlugin;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=ingestchain", "eureka.client.enabled=false" },
    locations = { "classpath:application-test.properties" })
public class IngestProcessingChainServiceIT extends IngestMultitenantServiceIT {

    @Autowired
    private IIngestProcessingChainService ingestProcessingService;

    @Autowired
    private IPluginService pluginService;

    @Test
    public void checkDefaultProcessingChain() {
        Page<IngestProcessingChain> results = ingestProcessingService.searchChains(IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                                                   PageRequest.of(0, 100));
        Assert.assertEquals(1, results.getTotalElements());
    }

    private IngestProcessingChain createBaseChain() throws ModuleException {
        IngestProcessingChain newChain = new IngestProcessingChain();
        newChain.setDescription("Ingest processing chain");
        newChain.setName("ipst_Chain1");

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
    public void createSingleProcessingChain() throws ModuleException {
        Assert.assertNotNull(createBaseChain().getId());
    }

    @Test
    public void updateProcessingChain() throws ModuleException {

        IngestProcessingChain chain = createBaseChain();
        String updatedLabel = "validationPlugin_ipst_update";
        chain.getValidationPlugin().setLabel(updatedLabel);
        ingestProcessingService.updateChain(chain);

        Optional<PluginConfiguration> conf = pluginService.findPluginConfigurationByLabel(updatedLabel);
        Assert.assertTrue(conf.isPresent());
        Assert.assertEquals(updatedLabel, conf.get().getLabel());
    }

    @Test
    public void deleteSingleProcessingChain() throws ModuleException {
        IngestProcessingChain chain = createBaseChain();
        Assert.assertNotNull(chain.getId());
        ingestProcessingService.deleteChain(chain.getName());
    }
}
