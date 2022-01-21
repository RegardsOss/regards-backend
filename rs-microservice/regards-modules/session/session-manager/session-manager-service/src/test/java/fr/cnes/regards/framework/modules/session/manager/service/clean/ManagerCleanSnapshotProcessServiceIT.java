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
package fr.cnes.regards.framework.modules.session.manager.service.clean;

import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.manager.service.AbstractManagerServiceUtilsTest;
import fr.cnes.regards.framework.modules.session.manager.service.clean.snapshotprocess.ManagerCleanSnapshotProcessService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link ManagerCleanSnapshotProcessService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=manager_clean_process_it",
        "regards.session.manager.clean.snapshot.process.limit.store=30" })
@ActiveProfiles({ "noscheduler" })
public class ManagerCleanSnapshotProcessServiceIT extends AbstractManagerServiceUtilsTest {

    private static OffsetDateTime UPDATE_DATE;

    @Value("${regards.session.manager.clean.snapshot.process.limit.store}")
    private int limitStoreSnapshotProcess;

    @Override
    public void doInit() {
        UPDATE_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(limitStoreSnapshotProcess);
    }

    @Test
    @Purpose("Test if old unused snapshot process are correctly deleted")
    public void cleanSnapshotProcessTest() {
        // create session step requests
        createSessionStep();

        // create snapshot process and launch clean
        List<SnapshotProcess> snapshotProcessCreated = createSnapshotProcess();
        managerCleanService.clean();

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

    private void createSessionStep() {
        List<SessionStep> stepRequests = new ArrayList<>();

        // ACQUISITION
        SessionStep step1 = new SessionStep("scan", SOURCE_1, SESSION_1, StepTypeEnum.ACQUISITION, new StepState());
        step1.setLastUpdateDate(UPDATE_DATE.minusMinutes(10L));
        stepRequests.add(step1);

        // REFERENCING
        SessionStep step2 = new SessionStep("oais", SOURCE_2, SESSION_2, StepTypeEnum.REFERENCING, new StepState());
        step2.setLastUpdateDate(UPDATE_DATE.minusMinutes(9L));
        stepRequests.add(step2);

        // STORAGE
        SessionStep step3 = new SessionStep("storage", SOURCE_3, SESSION_3, StepTypeEnum.STORAGE, new StepState());
        step3.setLastUpdateDate(UPDATE_DATE.minusMinutes(8L));
        stepRequests.add(step3);

        // SAVE
        this.sessionStepRepo.saveAll(stepRequests);
    }
}