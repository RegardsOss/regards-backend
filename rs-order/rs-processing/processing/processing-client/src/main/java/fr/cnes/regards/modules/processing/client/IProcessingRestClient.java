package fr.cnes.regards.modules.processing.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.dto.PBatchResponse;
import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import io.vavr.collection.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.*;

@RestClient(name = "rs-processing", contextId = "rs-processing.rest.client")
@RequestMapping(
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE }
)
public interface IProcessingRestClient {

    @GetMapping(path = PROCESS_PATH)
    ResponseEntity<List<PProcessDTO>> listAll();

    @GetMapping(path = PROCESS_PATH + "/{name}")
    ResponseEntity<PProcessDTO> findByName(@PathVariable("name") String processName);

    @PostMapping(path = BATCH_PATH)
    ResponseEntity<PBatchResponse> createBatch(@RequestBody PBatchRequest request);

    @GetMapping(path = MONITORING_EXECUTIONS_PATH)
    ResponseEntity<List<PExecution>> executions(
            @RequestParam String tenant,
            @RequestParam java.util.List<ExecutionStatus> status,
            Pageable page
    );
}
