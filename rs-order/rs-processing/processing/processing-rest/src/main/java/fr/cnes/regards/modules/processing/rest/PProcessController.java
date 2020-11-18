package fr.cnes.regards.modules.processing.rest;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.domain.service.IProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESS_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM;

@RestController
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "servlet", matchIfMissing = true)
@RequestMapping(
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE }
)
public class PProcessController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PProcessController.class);

    private final IProcessService processService;
    private final IRuntimeTenantResolver tenantResolver;

    public PProcessController(IProcessService processService, IRuntimeTenantResolver tenantResolver) {
        this.processService = processService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping(path = PROCESS_PATH)
    @ResourceAccess(
            description = "Find all registered processes",
            role = DefaultRole.REGISTERED_USER)
    public List<PProcessDTO> findAll() {
        String tenant = tenantResolver.getTenant();
        return processService.findByTenant(tenant).collectList().block();
    }

    @GetMapping(path= PROCESS_PATH + "/{" + PROCESS_BUSINESS_ID_PARAM + "}")
    @ResourceAccess(
            description = "Find process by their business uuid",
            role = DefaultRole.REGISTERED_USER)
    public PProcessDTO findByUuid(@PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processId) {
        String tenant = tenantResolver.getTenant();
        return processService.findByTenant(tenant).filter(p -> p.getProcessId().equals(processId))
            .next()
            .block();
    }

}
