package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.repository.IPProcessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ProcessServiceImpl implements IProcessService {

    private final IPProcessRepository processRepo;

    @Autowired
    public ProcessServiceImpl(IPProcessRepository processRepo) {
        this.processRepo = processRepo;
    }

    public Flux<PProcessDTO> findByTenant(String tenant) {
        return processRepo.findAllByTenant(tenant)
                .map(PProcessDTO::fromProcess);
    }

}
