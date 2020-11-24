package fr.cnes.regards.modules.processing.controller;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.service.IProcessPluginConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.*;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.USER_ROLE_PARAM;

@RestController
@RequestMapping(path = PROCESSPLUGIN_PATH + CONFIG_SUFFIX)
public class ProcessPluginConfigController implements IResourceController<ProcessPluginConfigurationRightsDTO> {

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IProcessPluginConfigService rightsConfigService;

    private final IResourceService resourceService;

    @Autowired
    public ProcessPluginConfigController(
            IRuntimeTenantResolver runtimeTenantResolver,
            IProcessPluginConfigService rightsConfigService,
            IResourceService resourceService
    ) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.rightsConfigService = rightsConfigService;
        this.resourceService = resourceService;
    }

    @GetMapping
    @ResourceAccess(description = "Find all registered configured processes", role = DefaultRole.REGISTERED_USER)
    public Collection<EntityModel<ProcessPluginConfigurationRightsDTO>> findAll(
            @RequestParam(required = false) String processNameLike
    ) {
        Flux<ProcessPluginConfigurationRightsDTO> allRightsPluginConfigs = rightsConfigService.findAllRightsPluginConfigs();
        Flux<ProcessPluginConfigurationRightsDTO> filteredRightsPluginConfigs;
        if (processNameLike != null) {
            filteredRightsPluginConfigs = allRightsPluginConfigs
                    .filter(dto -> dto.getPluginConfiguration().getLabel().matches("^.*" +  processNameLike + ".*$"));
        }
        else {
            filteredRightsPluginConfigs = allRightsPluginConfigs;
        }
        return filteredRightsPluginConfigs.map(this::toResource).collectList().block();
    }

    @PostMapping(
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ResourceAccess(description = "Create a process configuration from a plugin", role = DefaultRole.ADMIN)
    public EntityModel<ProcessPluginConfigurationRightsDTO> create(@RequestBody ProcessPluginConfigurationRightsDTO rightsDto) {
        String tenant = runtimeTenantResolver.getTenant();
        return toResource(rightsConfigService.create(tenant, rightsDto).block());
    }

    @GetMapping(path = BID_SUFFIX)
    @ResourceAccess(description = "Find a configured process by its business uuid", role = DefaultRole.REGISTERED_USER)
    public EntityModel<ProcessPluginConfigurationRightsDTO> findByBusinessId(
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId) {
        return toResource(rightsConfigService.findByBusinessId(processBusinessId).block());
    }

    @PutMapping(path = BID_SUFFIX)
    @ResourceAccess(description = "Update the given process with the given rights configuration",
            role = DefaultRole.ADMIN)
    public EntityModel<ProcessPluginConfigurationRightsDTO> update(@PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
            @RequestBody ProcessPluginConfigurationRightsDTO rightsDto) {
        String tenant = runtimeTenantResolver.getTenant();
        return toResource(rightsConfigService.update(tenant, processBusinessId, rightsDto).block());
    }

    @DeleteMapping(path = BID_SUFFIX)
    @ResourceAccess(description = "Delete the given process", role = DefaultRole.ADMIN)
    public EntityModel<ProcessPluginConfigurationRightsDTO> delete(@PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId) {
        String tenant = runtimeTenantResolver.getTenant();
        return toResource(rightsConfigService.delete(processBusinessId, tenant).block());
    }

    @PutMapping(path = BID_USERROLE_SUFFIX)
    @ResourceAccess(description = "Attache the given role to the given process", role = DefaultRole.ADMIN)
    public void attachRoleToProcess(@PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
            @RequestParam(USER_ROLE_PARAM) String userRole) {
        rightsConfigService.attachRoleToProcess(processBusinessId, userRole).block();
    }

    @Override
    public EntityModel<ProcessPluginConfigurationRightsDTO> toResource(final ProcessPluginConfigurationRightsDTO element, final Object... extras) {
        final EntityModel<ProcessPluginConfigurationRightsDTO> resource = resourceService.toResource(element);
        String businessIdStr = element.getPluginConfiguration().getBusinessId();
        UUID businessId = UUID.fromString(businessIdStr);
        resourceService.addLink(resource, this.getClass(), "findByBusinessId", LinkRels.SELF,
                MethodParamFactory.build(UUID.class, businessId));
        resourceService.addLink(resource, this.getClass(), "update", LinkRels.UPDATE,
                MethodParamFactory.build(UUID.class, businessId),
                MethodParamFactory.build(ProcessPluginConfigurationRightsDTO.class));
        if (rightsConfigService.canDelete(businessId).block()) {
            resourceService.addLink(resource, this.getClass(), "delete", LinkRels.DELETE,
                    MethodParamFactory.build(UUID.class, businessId));
        }
        return resource;
    }
}
