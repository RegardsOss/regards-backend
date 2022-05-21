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
package fr.cnes.regards.modules.storage.service.file.flow.performance;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.storage.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storage.domain.flow.StorageFlowItem;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import fr.cnes.regards.modules.storage.service.session.SessionNotifierPropertyEnum;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Test if the store workflow handles the creation of many files with the same checksum properly.
 * In this case, there must be as many owners as storage requests.
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(
    properties = { "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.default_schema=storage_flow_tests",
        "regards.amqp.enabled=true", "regards.storage.schedule.initial.delay=100", "regards.storage.schedule.delay=50",
        "regards.jobs.scan.delay=50" }, locations = { "classpath:application-test.properties" })
@ActiveProfiles({ "testAmqp" })
@Ignore("Performances tests")
public class StoreFileFlowItemMultipleTimesIT extends AbstractStorageIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowPerformanceIT.class);

    private static final String FILE_REF_OWNER = "owner";

    private static final String SESSION_OWNER = "SOURCE 1";

    private static final String SESSION = "SESSION 1";

    @Autowired
    private IFileStorageRequestRepository fileStorageRequestRepo;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepRepo;

    @Autowired
    private ISessionStepRepository sessionRepo;

    @Autowired
    private ISnapshotProcessRepository processRepo;

    @Before
    public void initialize() throws ModuleException {
        LOGGER.info("----- Tests initialization -----");

        // init
        super.init();
        stepRepo.deleteAll();
        sessionRepo.deleteAll();
        processRepo.deleteAll();

        // init online plugin plugin
        if (!storageLocationConfService.search(ONLINE_CONF_LABEL).isPresent()) {
            initDataStoragePluginConfiguration(ONLINE_CONF_LABEL, true);
        }
        storagePlgConfHandler.refresh();

        // simulate ready events
        simulateApplicationReadyEvent();
        simulateApplicationStartedEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @Test
    @Purpose("Test if all 'storedFile' notification are received in case of duplicated file (same checksum)")
    public void storeFileFlowItemMultipleTimes() throws InterruptedException {
        // init checksum, it must be the same for all files
        String checksum = UUID.randomUUID().toString();

        // create a new bus message of store requests
        List<StorageFlowItem> items = new ArrayList<>();
        int nbItems = 1000;

        for (int i = 0; i < nbItems; i++) {
            StorageFlowItem item = StorageFlowItem.build(FileStorageRequestDTO.build("file.name",
                                                                                     checksum,
                                                                                     "MD5",
                                                                                     "application/octet-stream",
                                                                                     FILE_REF_OWNER + i,
                                                                                     SESSION_OWNER,
                                                                                     SESSION,
                                                                                     originUrl,
                                                                                     ONLINE_CONF_LABEL,
                                                                                     Optional.empty()),
                                                         UUID.randomUUID().toString());
            items.add(item);
        }
        this.publisher.publish(items);

        // wait for the end of successful store request process
        Awaitility.await().atMost(200, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return fileStorageRequestRepo.findAll().size() == 0;
        });

        Thread.sleep(2_000);

        // check if there are as many notification requests as files stored
        Assert.assertEquals("Wrong number of storedFiles notification",
                            nbItems,
                            this.stepRepo.findAll()
                                         .stream()
                                         .filter(step -> step.getStepPropertyInfo()
                                                             .getProperty()
                                                             .equals(SessionNotifierPropertyEnum.STORED_FILES.getName()))
                                         .count());
    }
}