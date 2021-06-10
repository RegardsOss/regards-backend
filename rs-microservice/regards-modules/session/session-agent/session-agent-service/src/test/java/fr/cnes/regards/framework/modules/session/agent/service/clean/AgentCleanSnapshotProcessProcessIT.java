/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.session.agent.service.clean;

import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequestInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.service.clean.snapshotprocess.AgentCleanSnapshotProcessService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link AgentCleanSnapshotProcessService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=agent_clean_process_it",
        "regards.cipher.key-location=src/test/resources/testKey", "regards.cipher.iv=1234567812345678",
        "regards.session.agent.clean.snapshot.process.limit.store=30" })
@ActiveProfiles(value = { "noscheduler" })
public class AgentCleanSnapshotProcessProcessIT extends AbstractRegardsServiceTransactionalIT {

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyUpdateRequestRepo;

    @Autowired
    private ISnapshotProcessRepository snapshotProcessRepo;

    @Autowired
    private AgentCleanSnapshotProcessService agentCleanSnapshotProcessService;

    @Value("${regards.session.agent.clean.snapshot.process.limit.store}")
    private int limitStoreSnapshotProcess;

    private static final String SOURCE_1 = "SOURCE_1";

    private static final String SOURCE_2 = "SOURCE_2";

    private static final String SOURCE_3 = "SOURCE_3";

    private static final String SOURCE_4 = "SOURCE_4";

    private static final String SOURCE_5 = "SOURCE_5";

    private static final String SOURCE_6 = "SOURCE_6";

    private static OffsetDateTime UPDATE_DATE;

    @Before
    public void init() {
        UPDATE_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(limitStoreSnapshotProcess);
        this.snapshotProcessRepo.deleteAll();
        this.stepPropertyUpdateRequestRepo.deleteAll();
    }

    @Test
    @Purpose("Test if old unused snapshot process are correctly deleted")
    public void cleanSnapshotProcessTest() {
        // create requests
        createRunStepRequests();

        // create snapshot process and launch clean
        List<SnapshotProcess> snapshotProcessCreated = createSnapshotProcess();
        this.agentCleanSnapshotProcessService.clean();

        // Test result
        List<SnapshotProcess> snapshotProcessesRetrieved = this.snapshotProcessRepo.findAll();

        // SNAPSHOT linked to SOURCE_1, SOURCE_2, SOURCE_3 should be kept
        Assert.assertTrue("Snapshot process should have been present. It is linked to a step request.",
                          snapshotProcessesRetrieved.contains(snapshotProcessCreated.get(0)));
        Assert.assertTrue("Snapshot process should have been present. It is linked to a step request.",
                          snapshotProcessesRetrieved.contains(snapshotProcessCreated.get(1)));
        Assert.assertTrue("Snapshot process should have been present. It is linked to a step request. ",
                          snapshotProcessesRetrieved.contains(snapshotProcessCreated.get(2)));
        Assert.assertTrue("Snapshot process should not be present. It is not yet expired (cf. "
                                + "limitStoreSnapshotProcess).",
                           snapshotProcessesRetrieved.contains(snapshotProcessCreated.get(3)));

        // SNAPSHOT linked to SOURCE_5, SOURCE_6 should be removed
        Assert.assertFalse("Snapshot process should not be present. It is not linked to any step requests and it is "
                                   + "older than the maximum snapshot process storage date.",
                          snapshotProcessesRetrieved.contains(snapshotProcessCreated.get(4)));
        Assert.assertFalse("Snapshot process should not be present. It is not linked to any step requests.",
                          snapshotProcessesRetrieved.contains(snapshotProcessCreated.get(5)));

    }

    private List<SnapshotProcess> createSnapshotProcess() {
        List<SnapshotProcess> snapshotProcesses = new ArrayList<>();
        snapshotProcesses.add(new SnapshotProcess(SOURCE_1, UPDATE_DATE.minusDays(5), null));
        snapshotProcesses.add(new SnapshotProcess(SOURCE_2, UPDATE_DATE.plusDays(5), null));
        snapshotProcesses.add(new SnapshotProcess(SOURCE_3, null, null));
        snapshotProcesses.add(new SnapshotProcess(SOURCE_4, UPDATE_DATE.plusDays(5), null));
        snapshotProcesses.add(new SnapshotProcess(SOURCE_5, UPDATE_DATE.minusDays(5), null));
        snapshotProcesses.add(new SnapshotProcess(SOURCE_6, null, null));
        return this.snapshotProcessRepo.saveAll(snapshotProcesses);
    }

    private void createRunStepRequests() {
        List<StepPropertyUpdateRequest> stepRequests = new ArrayList<>();

        // ACQUISITION
        stepRequests.add(new StepPropertyUpdateRequest("scan", SOURCE_1, "OWNER_1", OffsetDateTime.now(),
                                                       StepPropertyEventTypeEnum.INC,

                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.ACQUISITION,
                                                                                         StepPropertyStateEnum.SUCCESS,
                                                                                         "gen.products", "1", true, false)));

        // REFERENCING
        stepRequests.add(new StepPropertyUpdateRequest("oais", SOURCE_2, "OWNER_2", OffsetDateTime.now(),
                                                       StepPropertyEventTypeEnum.INC,

                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.REFERENCING,
                                                                                         StepPropertyStateEnum.SUCCESS,
                                                                                         "gen.products", "1", true, false)));

        // STORAGE
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE_3, "OWNER_3", OffsetDateTime.now(),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.STORAGE,
                                                                                         StepPropertyStateEnum.SUCCESS,
                                                                                         "gen.products", "1", true, false)));
        // SAVE
        this.stepPropertyUpdateRequestRepo.saveAll(stepRequests);
    }
}