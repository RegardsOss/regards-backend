package fr.cnes.regards.modules.processing.rest;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.repository.IPExecutionRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

import static fr.cnes.regards.modules.processing.ProcessingConstants.ContentType.APPLICATION_JSON;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.MONITORING_EXECUTIONS_PATH;

@RestController
public class PMonitoringController {

    private final IPExecutionRepository execRepo;

    public PMonitoringController(
        IPExecutionRepository execRepo
    ) {
        this.execRepo = execRepo;
    }

    @GetMapping(path = MONITORING_EXECUTIONS_PATH,
                consumes = APPLICATION_JSON,
                produces = APPLICATION_JSON)
    public Flux<PExecution> executions(
            @RequestParam String tenant,
            @RequestParam List<ExecutionStatus> status,
            Pageable page
    ) {
        return execRepo.findByTenantAndCurrentStatusIn(tenant, status, page);
    }

}
