/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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


package fr.cnes.regards.modules.ingest.service.dump;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IDumpSettingsRepository;
import fr.cnes.regards.modules.ingest.domain.dump.DumpSettings;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import fr.cnes.regards.modules.ingest.service.schedule.AIPSaveMetadataScheduler;

/**
 * Test for {@link DumpManagerService}
 * @author Iliana Ghazali
 */

@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=dump_manager_it", "regards.amqp.enabled=true" },
        locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock", "noschedule" })
public class DumpManagerIT extends IngestMultitenantServiceTest {

    @Autowired
    private DumpManagerService dumpManagerService;

    @Autowired
    private AIPSaveMetadataScheduler saveMetadataScheduler;

    @Autowired
    private IDumpSettingsRepository dumpRepository;

    @Autowired
    private IAbstractRequestRepository requestRepository;

    @Autowired
    IJobInfoRepository jobInfoRepository;

    private String tenant;

    @Autowired

    @Override
    public void doInit() {
        simulateApplicationReadyEvent();
        // Re-set tenant because above simulation clear it!
        this.tenant = getDefaultTenant();
        runtimeTenantResolver.forceTenant(this.tenant);
        dumpRepository.deleteAll();
    }

    @Test
    @Purpose("Test update of a scheduler")
    public void testUpdateDumpAndScheduler() throws ExecutionException, InterruptedException, ModuleException {
        // Create new dump configuration and scheduler
        // activate task execution every minute
        dumpRepository.save(new DumpSettings(true, "0 * * * * *", "target/dump", null));
        saveMetadataScheduler.initAIPSaveMetaDataJobsSchedulers();

        // Update scheduler with a new dump configuration
        // change task execution every 10 seconds
        dumpManagerService.updateDumpAndScheduler(new DumpSettings(true, "*/10 * * * * *", "target/dump", null));

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
        // CHECK PARAMETER EXCEPTION
        // Test update dump with incorrect cron
        DumpSettings dumpSettings = new DumpSettings(true, "* * *", "target/dump", null);
        try {
            dumpManagerService.updateDumpAndScheduler(dumpSettings);
            Assert.fail(String.format("%s was expected", EntityInvalidException.class.getName()));
        } catch (ModuleException e) {
            LOGGER.error("Exception successfully thrown", e);
        }

        // CHECK NOT EXISTING DUMP EXCEPTION
        // Test update with dump settings that do not exist in db
        dumpSettings = new DumpSettings(true, "* * 0 * * *", "target/", null);
        try {
            dumpManagerService.updateDumpAndScheduler(dumpSettings);
            Assert.fail(String.format("%s was expected", EntityNotFoundException.class.getName()));
        } catch (ModuleException e) {
            LOGGER.error("Exception successfully thrown", e);
        }
    }

    @Override
    public void doAfter() {
        dumpRepository.deleteAll();
    }
}


