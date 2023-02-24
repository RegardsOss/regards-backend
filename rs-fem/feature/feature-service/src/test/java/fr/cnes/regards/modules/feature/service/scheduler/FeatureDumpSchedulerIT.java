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


package fr.cnes.regards.modules.feature.service.scheduler;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.dump.domain.DumpParameters;
import fr.cnes.regards.framework.modules.dump.service.settings.DumpSettingsService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceIT;
import fr.cnes.regards.modules.feature.service.task.FeatureSaveMetadataScheduler;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

/**
 * Test for {@link FeatureSaveMetadataScheduler}
 *
 * @author Iliana Ghazali
 */

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_savemetadata_job_it",
                                   "regards.amqp.enabled=true" })
@ActiveProfiles(value = { "testAmqp", "noFemHandler", "noscheduler" })
public class FeatureDumpSchedulerIT extends AbstractFeatureMultitenantServiceIT {

    private String tenant;

    @Autowired
    private FeatureSaveMetadataScheduler saveMetadataScheduler;

    @Autowired
    private DumpSettingsService dumpSettingsService;

    @Override
    public void doInit() {
        simulateApplicationReadyEvent();
        // Re-set tenant because above simulation clear it!
        this.tenant = getDefaultTenant();
        runtimeTenantResolver.forceTenant(this.tenant);
    }

    @Test
    @Purpose("Test update of a scheduler")
    public void testUpdateDumpAndScheduler() throws ExecutionException, InterruptedException, ModuleException {

        DumpParameters dumpParameters = new DumpParameters().setActiveModule(true)
                                                            .setDumpLocation("target/dump")
                                                            .setCronTrigger("0 * * * * *");

        dumpSettingsService.setDumpParameters(dumpParameters);

        dumpSettingsService.setDumpParameters(dumpParameters.setCronTrigger("*/10 * * * * *"));

        // Wait for scheduler execution
        // if the get() execution time exceeds trigger newly scheduled, then the new scheduler was not taken into account
        OffsetDateTime start = OffsetDateTime.now();
        ScheduledFuture scheduler = saveMetadataScheduler.getSchedulersByTenant().get(this.tenant);
        scheduler.get();
        OffsetDateTime executionDuration = OffsetDateTime.now();
        Assert.assertTrue("The scheduler was not updated because it was not executed with new cron trigger",
                          Duration.between(start, executionDuration).compareTo(Duration.ofSeconds(15)) < 0);
    }

    @Test
    @Purpose("Test update of a scheduler with an incorrect dump configuration")
    public void testUpdateDumpAndSchedulerError() {
        DumpParameters dumpParameters = new DumpParameters().setActiveModule(true)
                                                            .setDumpLocation("target/dump")
                                                            .setCronTrigger("* * *");
        try {
            dumpSettingsService.setDumpParameters(dumpParameters);
            Assert.fail(String.format("%s was expected", EntityInvalidException.class.getName()));
        } catch (ModuleException e) {
            LOGGER.error("Exception successfully thrown", e);
        }
    }

    @Override
    public void doAfter() throws EntityException {
        dumpSettingsService.resetSettings();
    }

}


