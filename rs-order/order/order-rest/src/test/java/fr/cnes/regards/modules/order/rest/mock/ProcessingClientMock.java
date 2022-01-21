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
package fr.cnes.regards.modules.order.rest.mock;

import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.processing.client.IProcessingRestClient;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.dto.ExecutionParamDTO;
import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.domain.dto.PBatchResponse;
import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterType;
import fr.cnes.regards.modules.processing.forecast.MultiplierResultSizeForecast;
import fr.cnes.regards.modules.processing.order.Cardinality;
import fr.cnes.regards.modules.processing.order.OrderProcessInfo;
import fr.cnes.regards.modules.processing.order.OrderProcessInfoMapper;
import fr.cnes.regards.modules.processing.order.Scope;
import fr.cnes.regards.modules.processing.order.SizeLimit;
import io.vavr.collection.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

/**
 * @author Iliana Ghazali
 */
public class ProcessingClientMock implements IProcessingRestClient {

    public static final String PROCESS_ID_FEATURES_2L = "123e4567-e89b-12d3-a456-426614174000";

    public static final String PROCESS_ID_FEATURES_5L = "223e4567-e89b-12d3-a456-426614174000";

    public static final String PROCESS_ID_BYTES_150L = "323e4567-e89b-12d3-a456-426614174000";

    public static final String PROCESS_ID_BYTES_300000L = "423e4567-e89b-12d3-a456-426614174000";

    public static final String PROCESS_ID_FILES_10L = "523e4567-e89b-12d3-a456-426614174000";

    public static final String PROCESS_ID_FILES_20L = "623e4567-e89b-12d3-a456-426614174000";

    public static final String PROCESS_ID_NO_LIMIT = "723e4567-e89b-12d3-a456-426614174000";

    private List<PProcessDTO> processes;

    public ProcessingClientMock() {
        createProcesses();
    }

    @Override
    public ResponseEntity<List<PProcessDTO>> listAll() {
        return new ResponseEntity<>(processes, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PProcessDTO> findByName(String processName) {
        // nothing to mock for now
        return null;
    }

    @Override
    public ResponseEntity<PProcessDTO> findByUuid(String processUUId) {
        PProcessDTO processDTOFound =
                Flux.fromIterable(processes).filter(p -> p.getProcessId().equals(UUID.fromString(processUUId))).next().block();
        return processDTOFound == null ? new ResponseEntity<>(HttpStatus.NOT_FOUND) : new ResponseEntity<>(processDTOFound, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PBatchResponse> createBatch(PBatchRequest request) {
        // nothing to mock for now
        return null;
    }

    @Override
    public ResponseEntity<List<PExecution>> executions(String tenant, java.util.List<ExecutionStatus> status, Pageable page) {
        // nothing to mock for now
        return null;
    }

    private void createProcesses() {
        processes = List.of(
                createProcessDTO(PROCESS_ID_FEATURES_2L, new OrderProcessInfo(
                        Scope.SUBORDER,
                        Cardinality.ONE_PER_EXECUTION,
                        List.of(DataType.RAWDATA),
                        new SizeLimit(SizeLimit.Type.FEATURES, 2L),
                        new MultiplierResultSizeForecast(1d), Boolean.TRUE
                )),
                createProcessDTO(PROCESS_ID_FEATURES_5L, new OrderProcessInfo(
                        Scope.SUBORDER,
                        Cardinality.ONE_PER_EXECUTION,
                        List.of(DataType.RAWDATA),
                        new SizeLimit(SizeLimit.Type.FEATURES, 5L),
                        new MultiplierResultSizeForecast(1d), Boolean.TRUE
                )),
                createProcessDTO(PROCESS_ID_BYTES_150L, new OrderProcessInfo(
                        Scope.SUBORDER,
                        Cardinality.ONE_PER_EXECUTION,
                        List.of(DataType.RAWDATA),
                        new SizeLimit(SizeLimit.Type.BYTES, 150L),
                        new MultiplierResultSizeForecast(1d), Boolean.TRUE
                )),
                createProcessDTO(PROCESS_ID_BYTES_300000L, new OrderProcessInfo(
                        Scope.SUBORDER,
                        Cardinality.ONE_PER_EXECUTION,
                        List.of(DataType.RAWDATA),
                        new SizeLimit(SizeLimit.Type.BYTES, 300000L),
                        new MultiplierResultSizeForecast(1d), Boolean.TRUE
                )),
                createProcessDTO(PROCESS_ID_FILES_10L, new OrderProcessInfo(
                        Scope.SUBORDER,
                        Cardinality.ONE_PER_EXECUTION,
                        List.of(DataType.RAWDATA),
                        new SizeLimit(SizeLimit.Type.FILES, 10L),
                        new MultiplierResultSizeForecast(1d), Boolean.TRUE
                )),
                createProcessDTO(PROCESS_ID_FILES_20L, new OrderProcessInfo(
                        Scope.SUBORDER,
                        Cardinality.ONE_PER_EXECUTION,
                        List.of(DataType.RAWDATA),
                        new SizeLimit(SizeLimit.Type.FILES, 20L),
                        new MultiplierResultSizeForecast(1d), Boolean.TRUE
                )),
                createProcessDTO(PROCESS_ID_NO_LIMIT, new OrderProcessInfo(
                        Scope.SUBORDER,
                        Cardinality.ONE_PER_EXECUTION,
                        List.of(DataType.RAWDATA),
                        new SizeLimit(SizeLimit.Type.NO_LIMIT, 0L),
                        new MultiplierResultSizeForecast(1d), Boolean.TRUE
                )));
    }

    private PProcessDTO createProcessDTO(String processUUID, OrderProcessInfo orderProcessInfo) {
        OrderProcessInfoMapper processInfoMapper = new OrderProcessInfoMapper();
        return new PProcessDTO(
                UUID.fromString(processUUID),
                "the-process-name",
                true,
                processInfoMapper.toMap(orderProcessInfo),
                List.of(new ExecutionParamDTO("the-param-name", ExecutionParameterType.STRING,
                        "The param desc")));
    }
}
