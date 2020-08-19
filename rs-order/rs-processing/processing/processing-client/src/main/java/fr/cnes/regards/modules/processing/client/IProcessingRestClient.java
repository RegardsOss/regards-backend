package fr.cnes.regards.modules.processing.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.dto.PBatchResponse;
import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.dto.PProcessPutDTO;
import io.vavr.collection.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import static fr.cnes.regards.modules.processing.ProcessingConstants.ContentType.APPLICATION_JSON;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.*;

@RestClient(name = "rs-processing", contextId = "rs-processing.rest.client")
public interface IProcessingRestClient {

    @GetMapping(
        path = PROCESS_PATH,
        produces = APPLICATION_JSON
    )
    ResponseEntity<List<PProcessDTO>> listAll();

    @GetMapping(
            path = PROCESS_PATH + "/{name}",
            produces = APPLICATION_JSON
    )
    ResponseEntity<PProcessDTO> findByName(@PathVariable("name") String processName);

    @PostMapping(
            path = BATCH_PATH,
            consumes = APPLICATION_JSON,
            produces = APPLICATION_JSON
    )
    ResponseEntity<PBatchResponse> createBatch(@RequestBody PBatchRequest request);

    @GetMapping(
            path = MONITORING_EXECUTIONS_PATH,
            consumes = APPLICATION_JSON,
            produces = APPLICATION_JSON
    )
    ResponseEntity<List<PExecution>> executions(
            @RequestParam String tenant,
            @RequestParam java.util.List<ExecutionStatus> status,
            Pageable page
    );
}
