package fr.cnes.regards.framework.modules.tenant.settings.rest;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSettingDto;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestController
@RequestMapping(path = DynamicTenantSettingController.ROOT_PATH)
public class DynamicTenantSettingController implements IResourceController<DynamicTenantSettingDto> {

    public static final String ROOT_PATH = "/settings";

    public static final String NAME_PATH = "/{name}";

    @Autowired
    private IDynamicTenantSettingService dynamicTenantSettingService;

    @Autowired
    private IResourceService resourceService;

    @PutMapping(path = NAME_PATH)
    @ResponseBody
    @ResourceAccess(description = "Allows to update a dynamic tenant setting", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<DynamicTenantSettingDto>> update(@PathVariable(name = "name") String name,
            @RequestBody DynamicTenantSettingDto setting)
            throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        return new ResponseEntity<>(toResource(new DynamicTenantSettingDto(dynamicTenantSettingService
                                                                                   .update(name, setting.getValue()))),
                                    HttpStatus.OK);
    }

    @DeleteMapping(path = NAME_PATH)
    @ResponseBody
    @ResourceAccess(description = "Allows to reset a dynamic tenant setting", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<DynamicTenantSettingDto>> reset(@PathVariable(name = "name") String name)
            throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        return new ResponseEntity<>(toResource(new DynamicTenantSettingDto(dynamicTenantSettingService.reset(name))),
                                    HttpStatus.OK);
    }

    @GetMapping
    @ResponseBody
    @ResourceAccess(description = "Allows to retrieve dynamic tenant settings", role = DefaultRole.ADMIN)
    public ResponseEntity<List<EntityModel<DynamicTenantSettingDto>>> retrieveAll(
            @RequestParam(name = "names", required = false) Set<String> names) {
        if (names == null || names.isEmpty()) {
            return new ResponseEntity<>(toResources(dynamicTenantSettingService.readAll().stream()
                                                            .map(DynamicTenantSettingDto::new)
                                                            .collect(Collectors.toSet())), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(toResources(dynamicTenantSettingService.readAll(names).stream()
                                                            .map(DynamicTenantSettingDto::new)
                                                            .collect(Collectors.toSet())), HttpStatus.OK);
        }
    }

    @Override
    public EntityModel<DynamicTenantSettingDto> toResource(DynamicTenantSettingDto element, Object... extras) {
        EntityModel<DynamicTenantSettingDto> resource = resourceService.toResource(element);
        resourceService
                .addLink(resource, this.getClass(), "retrieveAll", LinkRels.SELF, MethodParamFactory.build(Set.class));
        if (dynamicTenantSettingService.canUpdate(element.getName())) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "update",
                                    LinkRels.UPDATE,
                                    MethodParamFactory.build(String.class, element.getName()),
                                    MethodParamFactory.build(DynamicTenantSettingDto.class));
            if (!Objects.equals(element.getDefaultValue(), element.getValue())) {
                resourceService.addLink(resource,
                                        this.getClass(),
                                        "reset",
                                        LinkRelation.of("reset"),
                                        MethodParamFactory.build(String.class, element.getName()));
            }
        }
        return resource;
    }
}
