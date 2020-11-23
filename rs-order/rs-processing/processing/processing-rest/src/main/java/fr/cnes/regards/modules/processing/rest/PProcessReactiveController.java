package fr.cnes.regards.modules.processing.rest;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESS_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.domain.service.IProcessService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "reactive")
@RequestMapping(path = PROCESS_PATH)
public class PProcessReactiveController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PProcessReactiveController.class);

    @Autowired
    private IProcessService processService;

    @GetMapping
    public Flux<PProcessDTO> findAll() {
        return ReactiveSecurityContextHolder.getContext().flatMapMany(ctx -> {
            JWTAuthentication authentication = (JWTAuthentication) ctx.getAuthentication();
            String tenant = authentication.getTenant();
            return processService.findByTenant(tenant);
        });
    }

    @GetMapping(path = "/{name}")
    public Mono<PProcessDTO> findByUuid(@PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processId) {
        return ReactiveSecurityContextHolder.getContext().flatMap(ctx -> {
            JWTAuthentication authentication = (JWTAuthentication) ctx.getAuthentication();
            String tenant = authentication.getTenant();
            return processService.findByTenant(tenant).filter(p -> p.getProcessId().equals(processId)).next();
        });
    }

}
