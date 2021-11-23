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

import fr.cnes.regards.framework.modules.session.agent.client.ISessionAgentClient;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestDTO;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestsInfo;
import fr.cnes.regards.modules.workermanager.dto.requests.SessionsRequestsInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private static final String GLOBAL_SESSION_STEP = "workers";

    private ISessionAgentClient sessionNotificationClient;

    public SessionService(ISessionAgentClient sessionNotificationClient) {
        this.sessionNotificationClient = sessionNotificationClient;
    }

    public static String getSessionPropertyName(RequestDTO request, WorkerStepPropertyEnum property) {
        return getSessionPropertyName(request.getDispatchedWorkerType(), property);
    }

    public static String getSessionPropertyName(String workerType, WorkerStepPropertyEnum property) {
        return String.format(property.getPropertyPath(), workerType);
    }

    public void notifyNewRequests(SessionsRequestsInfo infoPerSession) {
        infoPerSession.keySet().forEach(key -> {
            String source = key.getLeft();
            String session = key.getRight();
            RequestsInfo sessionInfo = infoPerSession.get(key);

            Map<String, StepProperty> propertySteps = new HashMap<>();
            preparePropertyStep(source, session, WorkerStepPropertyEnum.TOTAL_REQUESTS.getPropertyPath(),
                                WorkerStepPropertyEnum.TOTAL_REQUESTS, propertySteps,
                                sessionInfo.getRequests().values().stream().mapToInt(Collection::size).sum());
            sendSteps(propertySteps);
        });
    }

    public void notifyScannedRequests(SessionsRequestsInfo infoPerSession, RequestStatus newStatus) {
        WorkerStepPropertyEnum.parse(newStatus).ifPresent(stepProperty -> {
            infoPerSession.keySet().forEach(key -> {
                String source = key.getLeft();
                String session = key.getRight();
                RequestsInfo sessionInfo = infoPerSession.get(key);

                Map<String, StepProperty> propertySteps = new HashMap<>();
                preparePropertyStepsForSession(source, session, sessionInfo, propertySteps, false);

                List<RequestDTO> requests = sessionInfo.getRequests().values().stream().flatMap(Collection::stream).collect(Collectors.toList());
                preparePropertyStepForRequests(source, session, requests, stepProperty, propertySteps, true);
                sendSteps(propertySteps);
            });
        });
    }

    public void notifyDelete(SessionsRequestsInfo infoPerSession) {
        infoPerSession.keySet().forEach(key -> {
            String source = key.getLeft();
            String session = key.getRight();
            RequestsInfo sessionInfo = infoPerSession.get(key);

            Map<String, StepProperty> propertySteps = new HashMap<>();
            preparePropertyStepsForSession(source, session, sessionInfo, propertySteps, false);

            preparePropertyStep(source, session, WorkerStepPropertyEnum.TOTAL_REQUESTS.getPropertyPath(),
                                WorkerStepPropertyEnum.TOTAL_REQUESTS, propertySteps,
                                -sessionInfo.getRequests().values().stream().mapToInt(Collection::size).sum());
            sendSteps(propertySteps);
        });
    }

    public void notifySessions(SessionsRequestsInfo infoPerSession, SessionsRequestsInfo newInfoPerSession) {
        newInfoPerSession.keySet().forEach(key -> {

            String source = key.getLeft();
            String session = key.getRight();
            RequestsInfo sessionInfo = infoPerSession.get(key);
            RequestsInfo newSessionInfo = newInfoPerSession.get(key);

            Map<String, StepProperty> propertySteps = new HashMap<>();
            preparePropertyStepsForSession(source, session, sessionInfo, propertySteps, false);
            preparePropertyStepsForSession(source, session, newSessionInfo, propertySteps, true);

            sendSteps(propertySteps);
        });
    }

    private void preparePropertyStepsForSession(String source, String session, RequestsInfo sessionInfo,
            Map<String, StepProperty> propertySteps, boolean inc) {
        for (RequestStatus status : RequestStatus.values()) {
            WorkerStepPropertyEnum.parse(status).ifPresent(stepProperty -> {
                preparePropertyStepForRequests(source, session,
                                               sessionInfo.getRequests().get(status), stepProperty,
                                               propertySteps, inc);
            });
        }
    }

    private void preparePropertyStepForRequests(String source, String session, Collection<RequestDTO> requests,
            WorkerStepPropertyEnum propertyType, Map<String, StepProperty> propertySteps, boolean inc) {
        if (requests != null && !requests.isEmpty()) {
            requests.forEach(
                    request -> this.preparePropertyStepForRequest(source, session, request, propertyType, propertySteps,
                                                                  inc));
        }
    }

    private void preparePropertyStepForRequest(String source, String session, RequestDTO request,
            WorkerStepPropertyEnum propertyType, Map<String, StepProperty> propertySteps, boolean inc) {
        preparePropertyStepForRequest(source, session, getSessionPropertyName(request, propertyType), propertyType, propertySteps, inc);
    }

    private void preparePropertyStepForRequest(String source, String session, String newPropertyPath,
            WorkerStepPropertyEnum propertyType, Map<String, StepProperty> propertySteps, boolean inc) {
        propertySteps.compute(newPropertyPath, (propertyPath, property) -> {
            if (property == null) {
                Integer intValue = inc ? 1 : -1;
                return createStep(source, session, propertyPath, intValue.toString(), propertyType);
            } else {
                String pptValue = property.getStepPropertyInfo().getValue();
                Integer intValue = inc ? Integer.valueOf(pptValue) + 1 : Integer.valueOf(pptValue) - 1;
                property.getStepPropertyInfo().setValue(intValue.toString());
                return property;
            }
        });
    }

    private void preparePropertyStep(String source, String session, String newPropertyPath,
            WorkerStepPropertyEnum propertyType, Map<String, StepProperty> propertySteps, Integer value) {
        propertySteps.put(newPropertyPath, createStep(source, session, newPropertyPath, value.toString(), propertyType));
    }


    private StepProperty createStep(String source, String session, String propertyPath, String value,
            WorkerStepPropertyEnum propertyType) {
        return new StepProperty(GLOBAL_SESSION_STEP, source, session,
                                new StepPropertyInfo(StepTypeEnum.ACQUISITION, propertyType.getPropertyState(),
                                                     propertyPath, value, propertyType.isInputRelated(),
                                                     propertyType.isOutputRelated()));
    }

    private void sendSteps(Map<String, StepProperty> propertySteps) {
        propertySteps.values().stream()
                .filter(ppt -> Integer.valueOf(ppt.getStepPropertyInfo().getValue()) != 0)
                .forEach(ppt -> {
                    int value = Integer.parseInt(ppt.getStepPropertyInfo().getValue());
                    if (value > 0 ) {
                        sessionNotificationClient.increment(ppt);
                    } else {
                        ppt.getStepPropertyInfo().setValue(String.valueOf(-value));
                        sessionNotificationClient.decrement(ppt);
                    }
                } );
    }

}
