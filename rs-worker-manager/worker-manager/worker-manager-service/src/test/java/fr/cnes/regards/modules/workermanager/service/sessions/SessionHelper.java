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
package fr.cnes.regards.modules.workermanager.service.sessions;

import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SessionHelper {

    private SessionHelper() { }

    public static void checkSession(IStepPropertyUpdateRequestRepository repo, String source, String session, String workerType, int total, int running,
            int noWorkerAvailable, int dispatched, int success, int error, int invalid, int retry, int deletion) {

        try {
            // Wait for handler to handle session events
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Assert.fail("Fails !!");
        }
        Map<String, List<StepPropertyUpdateRequest>> stepProperties = repo.findBySession(
                        session).stream().filter(s -> s.getSource().equals(source))
                .collect(Collectors.groupingBy(s -> s.getStepPropertyInfo().getProperty()));

        checkSessionProperty(workerType, stepProperties, WorkerStepPropertyEnum.TOTAL_REQUESTS, total);
        checkSessionProperty(workerType, stepProperties, WorkerStepPropertyEnum.RUNNING, running);
        checkSessionProperty(workerType, stepProperties, WorkerStepPropertyEnum.NO_WORKER_AVAILABLE, noWorkerAvailable);
        checkSessionProperty(workerType, stepProperties, WorkerStepPropertyEnum.DISPATCHED, dispatched);
        checkSessionProperty(workerType, stepProperties, WorkerStepPropertyEnum.DONE, success);
        checkSessionProperty(workerType, stepProperties, WorkerStepPropertyEnum.ERROR, error);
        checkSessionProperty(workerType, stepProperties, WorkerStepPropertyEnum.INVALID_CONTENT, invalid);
        checkSessionProperty(workerType, stepProperties, WorkerStepPropertyEnum.RETRY_PENDING, retry);
        checkSessionProperty(workerType, stepProperties, WorkerStepPropertyEnum.DELETION_PENDING, deletion);

    }

    private static void checkSessionProperty(String workerType, Map<String, List<StepPropertyUpdateRequest>> stepProperties,
            WorkerStepPropertyEnum property, int expected) {
        String propertyName = SessionService.getSessionPropertyName(workerType, property);
        int count = stepProperties.getOrDefault(propertyName, new ArrayList<>()).stream()
                .mapToInt(s -> s.getType() == StepPropertyEventTypeEnum.INC ?
                        Integer.parseInt(s.getStepPropertyInfo().getValue()) :
                        -Integer.parseInt(s.getStepPropertyInfo().getValue()))
                .reduce(0 , (total, value) -> total + value);
        Assert.assertEquals(String.format("Invalid number of %s requests in session", property), expected, count);
    }

}
