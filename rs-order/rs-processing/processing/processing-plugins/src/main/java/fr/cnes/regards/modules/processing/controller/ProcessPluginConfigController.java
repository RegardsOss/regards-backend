package fr.cnes.regards.modules.processing.controller;

import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.service.IProcessPluginConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static fr.cnes.regards.modules.processing.ProcessingConstants.ContentType.APPLICATION_JSON;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.*;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM;

@RestController
public class ProcessPluginConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigController.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;
    private final IProcessPluginConfigService rightsConfigService;

    @Autowired
    public ProcessPluginConfigController(
            IRuntimeTenantResolver runtimeTenantResolver,
            IProcessPluginConfigService rightsConfigService
    ) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.rightsConfigService = rightsConfigService;
    }

    @GetMapping(
            path = PROCESS_CONFIG_PATH,
            produces = APPLICATION_JSON
    )
    public Flux<ProcessPluginConfigurationRightsDTO> findAll() {
        return getContext()
                .flatMapMany(sc -> {
                    String tenant = ((JWTAuthentication) sc.getAuthentication()).getTenant();
                    runtimeTenantResolver.forceTenant(tenant);
                    return rightsConfigService.findAllRightsPluginConfigs();
                });
    }


    @GetMapping(
            path = PROCESS_CONFIG_BID_PATH,
            produces = APPLICATION_JSON
    )
    public Mono<ProcessPluginConfigurationRightsDTO> findByBusinessId(
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId
    ) {
        return getContext()
                .flatMap(sc -> {
                    String tenant = ((JWTAuthentication) sc.getAuthentication()).getTenant();
                    runtimeTenantResolver.forceTenant(tenant);
                    return rightsConfigService.findByBusinessId(processBusinessId);
                });
    }

    @PostMapping(
            path = PROCESS_CONFIG_PATH,
            produces = APPLICATION_JSON,
            consumes = APPLICATION_JSON
    )
    public Mono<ProcessPluginConfigurationRightsDTO> save(
            @RequestBody ProcessPluginConfigurationRightsDTO rightsDto
    ) {
        return getContext()
                .flatMap(sc -> {
                    String tenant = ((JWTAuthentication) sc.getAuthentication()).getTenant();
                    runtimeTenantResolver.forceTenant(tenant);
                    return rightsConfigService.save(tenant, rightsDto);
                });
    }

    @PutMapping(
            path = PROCESS_CONFIG_BID_PATH,
            produces = APPLICATION_JSON,
            consumes = APPLICATION_JSON
    )
    public Mono<ProcessPluginConfigurationRightsDTO> update(
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
            @RequestBody ProcessPluginConfigurationRightsDTO rightsDto
    ) {
        return getContext()
                .flatMap(sc -> {
                    String tenant = ((JWTAuthentication) sc.getAuthentication()).getTenant();
                    runtimeTenantResolver.forceTenant(tenant);
                    return rightsConfigService.update(tenant, processBusinessId, rightsDto);
                });
    }

    @DeleteMapping(
            path = PROCESS_CONFIG_BID_PATH,
            produces = APPLICATION_JSON
    )
    public Mono<ProcessPluginConfigurationRightsDTO> save(
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId
    ) {
        return getContext()
                .flatMap(sc -> {
                    String tenant = ((JWTAuthentication) sc.getAuthentication()).getTenant();
                    runtimeTenantResolver.forceTenant(tenant);
                    return rightsConfigService.delete(processBusinessId);
                });
    }

    @GetMapping(
            path = PROCESS_METADATA_PATH,
            produces = APPLICATION_JSON
    )
    public Flux<PluginMetaData> listAllDetectedPlugins() {
        return getContext()
            .flatMapMany(ctx -> Flux.fromIterable(PluginUtils.getPlugins().values())
                .doOnError(t -> LOGGER.error(t.getMessage(), t)));
    }

    private Mono<SecurityContext> getContext() {
        return ReactiveSecurityContextHolder.getContext()
                .switchIfEmpty(Mono.defer(() -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Action requires JWT token"))));
    }
}
