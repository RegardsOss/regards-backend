/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;

import java.util.Arrays;
import java.util.Optional;

public enum WorkerStepPropertyEnum {

    TOTAL_REQUESTS("workers.requests", null, StepPropertyStateEnum.INFO, false, true, false),
    NO_WORKER_AVAILABLE("workers.no_worker_available",
                        RequestStatus.NO_WORKER_AVAILABLE,
                        StepPropertyStateEnum.WAITING,
                        false,
                        false,
                        false),
    RUNNING("workers.%s.running", RequestStatus.RUNNING, StepPropertyStateEnum.RUNNING, true, false, false),
    DISPATCHED("workers.%s.dispatched", RequestStatus.DISPATCHED, StepPropertyStateEnum.RUNNING, true, false, false),
    ERROR("workers.%s.error", RequestStatus.ERROR, StepPropertyStateEnum.ERROR, true, false, false),
    INVALID_CONTENT("workers.%s.invalid",
                    RequestStatus.INVALID_CONTENT,
                    StepPropertyStateEnum.ERROR,
                    true,
                    false,
                    false),
    DONE("workers.%s.done", RequestStatus.SUCCESS, StepPropertyStateEnum.SUCCESS, true, false, true),
    RETRY_PENDING("workers.%s.retry", RequestStatus.TO_DISPATCH, StepPropertyStateEnum.INFO, true, false, false),
    DELETION_PENDING("workers.%s.deletion", RequestStatus.TO_DELETE, StepPropertyStateEnum.INFO, true, false, false);

    private final String propertyPath;

    private final boolean workerTypeRequired;

    private final RequestStatus requestStatus;

    private final StepPropertyStateEnum propertyState;

    private final boolean inputRelated;

    private final boolean outputRelated;

    WorkerStepPropertyEnum(String propertyPath,
                           RequestStatus requestStatus,
                           StepPropertyStateEnum propertyState,
                           boolean workerTypeRequired,
                           boolean inputRelated,
                           boolean outputRelated) {
        this.propertyPath = propertyPath;
        this.requestStatus = requestStatus;
        this.propertyState = propertyState;
        this.workerTypeRequired = workerTypeRequired;
        this.inputRelated = inputRelated;
        this.outputRelated = outputRelated;
    }

    public static Optional<WorkerStepPropertyEnum> parse(RequestStatus status) {
        Optional<WorkerStepPropertyEnum> oStep = Arrays.stream(WorkerStepPropertyEnum.values())
                                                       .filter(step -> step.getRequestStatus() != null
                                                                       && status == step.getRequestStatus())
                                                       .findFirst();
        return oStep;
    }

    public String getPropertyPath() {
        return this.propertyPath;
    }

    public RequestStatus getRequestStatus() {
        return this.requestStatus;
    }

    public StepPropertyStateEnum getPropertyState() {
        return this.propertyState;
    }

    public boolean isInputRelated() {
        return inputRelated;
    }

    public boolean isOutputRelated() {
        return outputRelated;
    }

    public boolean isWorkerTypeRequired() {
        return workerTypeRequired;
    }

}
