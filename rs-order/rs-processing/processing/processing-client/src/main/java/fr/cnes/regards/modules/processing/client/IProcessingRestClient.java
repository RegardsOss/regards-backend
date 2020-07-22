package fr.cnes.regards.modules.processing.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.dto.PProcessPutDTO;
import io.vavr.collection.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static fr.cnes.regards.modules.processing.ProcessingConstants.ContentType.APPLICATION_JSON;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESSES_PATH;

@RestClient(name = "rs-processing", contextId = "rs-processing.rest.client")
public interface IProcessingRestClient {

    @RequestMapping(method = RequestMethod.GET, path = PROCESSES_PATH, produces = APPLICATION_JSON)
    List<PProcessDTO> listAll();

    @RequestMapping(method = RequestMethod.PUT, path = PROCESSES_PATH, produces = APPLICATION_JSON)
    String setProcessProperties(@RequestBody PProcessPutDTO data);

}
