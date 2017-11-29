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

package fr.cnes.regards.modules.acquisition.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.service.conf.ChainGenerationServiceConfiguration;
import fr.cnes.regards.modules.acquisition.service.conf.MockedFeignClientConf;
import fr.cnes.regards.modules.acquisition.service.step.AbstractAcquisitionIT;

/**
 * @author Christophe Mertz
 *
 */
@ContextConfiguration(classes = { ChainGenerationServiceConfiguration.class, MockedFeignClientConf.class })
@ActiveProfiles({ "test" })
@DirtiesContext
public class ScheduledStartChainIT extends AbstractAcquisitionIT {

    @Value("${regards.acquisition.process.run.chains.delay}")
    private String scheduledTasksDelay;

    @Before
    public void init() {
    }

    @Test
    public void startScheduledChainsAnyChainActive() throws InterruptedException {
        chain.setActive(false);
        chainService.save(chain);

        Assert.assertEquals(1, chainService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(0, chainService.findByActiveTrueAndRunningFalse().size());
        Assert.assertFalse(chainService.retrieve(chain.getId()).isActive());
        Assert.assertFalse(chainService.retrieve(chain.getId()).isRunning());
        Assert.assertEquals(0,
                            productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING,
                                                                   ProductStatus.COMPLETED, ProductStatus.FINISHED)
                                    .size());

        Thread.sleep(Integer.parseInt(scheduledTasksDelay) + 1_000);

        Assert.assertEquals(0,
                            productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING,
                                                                   ProductStatus.COMPLETED, ProductStatus.FINISHED)
                                    .size());
        Assert.assertEquals(0, chainService.findByActiveTrueAndRunningFalse().size());
        Assert.assertFalse(chainService.retrieve(chain.getId()).isActive());
        Assert.assertFalse(chainService.retrieve(chain.getId()).isRunning());

        Assert.assertTrue(runnings.isEmpty());
        Assert.assertTrue(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());
    }

    @Test
    public void startScheduledChainsOneChainActive() throws InterruptedException {
        Assert.assertEquals(1, chainService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(1, chainService.findByActiveTrueAndRunningFalse().size());
        Assert.assertTrue(chainService.retrieve(chain.getId()).isActive());
        Assert.assertFalse(chainService.retrieve(chain.getId()).isRunning());
        Assert.assertEquals(0,
                            productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING,
                                                                   ProductStatus.COMPLETED, ProductStatus.FINISHED)
                                    .size());

        Thread.sleep(Integer.parseInt(scheduledTasksDelay) + 1_000);

        Assert.assertEquals(0,
                            productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING,
                                                                   ProductStatus.COMPLETED, ProductStatus.FINISHED)
                                    .size());
        Assert.assertEquals(1, chainService.findByActiveTrueAndRunningFalse().size());
        Assert.assertTrue(chainService.retrieve(chain.getId()).isActive());
        Assert.assertFalse(chainService.retrieve(chain.getId()).isRunning());
    }

}
