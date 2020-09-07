package fr.cnes.regards.modules.processing.domain.service;

import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import reactor.core.publisher.Flux;

public interface IProcessService {

    Flux<PProcessDTO> findByTenant(String tenant);

}
