package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.utils.Unit;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Try;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IProcessService {

    Flux<PProcessDTO> findByTenant(String tenant);

}
