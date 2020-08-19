package fr.cnes.regards.modules.processing.rest;

import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.dto.PProcessPutDTO;
import fr.cnes.regards.modules.processing.service.IProcessService;
import fr.cnes.regards.modules.processing.utils.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.modules.processing.ProcessingConstants.ContentType.APPLICATION_JSON;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESS_PATH;

@RestController
public class PProcessController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PProcessController.class);

    @Autowired private IProcessService processService;

    @GetMapping(path = PROCESS_PATH, produces = APPLICATION_JSON)
    public Flux<PProcessDTO> findAll() {
        return processService.listAll();
    }

    @GetMapping(path= PROCESS_PATH + "/{name}", produces = APPLICATION_JSON)
    public Mono<PProcessDTO> findByName(@PathVariable("name") String processName) {
        return processService.listAll().filter(p -> p.getName().equals(processName)).next();
    }

}
