package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.dto.ExecutionParamDTO;
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

    @Override public Flux<PProcessDTO> listAll() {
        return processRepo.findAll()
                .map(p -> new PProcessDTO(
                    p.getProcessName(),
                    p.isActive(),
                    p.getAllowedTenants().values().toList(),
                    p.getAllowedUsersRoles().values().toList(),
                    p.getAllowedDatasets().values().toList(),
                    p.getParameters().map(param -> new ExecutionParamDTO(param.getName(), param.getType(), param.getDesc())).toList()));
    }

}
