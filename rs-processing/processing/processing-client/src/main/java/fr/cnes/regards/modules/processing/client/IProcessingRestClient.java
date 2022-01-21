/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.client;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.BATCH_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.MONITORING_EXECUTIONS_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESS_PATH;

import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.domain.dto.PBatchResponse;
import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import io.vavr.collection.List;

/**
 * Rest client to access the process endpoints allowing to
 * - search processes,
 * - get process info,
 * - create batches,
 * - monitor executions.
 *
 * @author gandrieu
 */
@RestClient(name = "rs-processing", contextId = "rs-processing.rest.client")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
public interface IProcessingRestClient {

    @GetMapping(path = PROCESS_PATH)
    ResponseEntity<List<PProcessDTO>> listAll();

    @GetMapping(path = PROCESS_PATH + "/{name}")
    ResponseEntity<PProcessDTO> findByName(@PathVariable("name") String processName);

    @GetMapping(path = PROCESS_PATH + "/{uuid}")
    ResponseEntity<PProcessDTO> findByUuid(@PathVariable("uuid") String processName);

    @PostMapping(path = BATCH_PATH)
    ResponseEntity<PBatchResponse> createBatch(@RequestBody PBatchRequest request);

    @GetMapping(path = MONITORING_EXECUTIONS_PATH)
    ResponseEntity<List<PExecution>> executions(@RequestParam String tenant,
            @RequestParam java.util.List<ExecutionStatus> status, Pageable page);
}
