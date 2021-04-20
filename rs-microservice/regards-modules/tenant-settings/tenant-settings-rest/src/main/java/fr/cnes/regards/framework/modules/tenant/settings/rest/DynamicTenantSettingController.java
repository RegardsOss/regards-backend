package fr.cnes.regards.framework.modules.tenant.settings.rest;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestController
@RequestMapping(path = DynamicTenantSettingController.ROOT_PATH)
public class DynamicTenantSettingController implements IResourceController<DynamicTenantSetting> {

    public static final String ROOT_PATH = "/settings";

    public static final String UPDATE_PATH = "/{name}";

    @Autowired
    private IDynamicTenantSettingService dynamicTenantSettingService;

    @PutMapping(path = UPDATE_PATH)
    @ResponseBody
    @ResourceAccess(description = "Allows to update a dynamic tenant setting", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<DynamicTenantSetting>> update(@PathVariable(name = "name") String name,
            @RequestBody DynamicTenantSetting setting)
            throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        return new ResponseEntity<>(toResource(dynamicTenantSettingService.update(name, setting.getValue())),
                                    HttpStatus.OK);
    }

    @GetMapping
    @ResponseBody
    @ResourceAccess(description = "Allows to retrieve dynamic tenant settings", role = DefaultRole.ADMIN)
    public ResponseEntity<List<EntityModel<DynamicTenantSetting>>> retrieveAll(
            @RequestParam(name = "names", required = false) Set<String> names) {
        if (names == null || names.isEmpty()) {
            return new ResponseEntity<>(toResources(dynamicTenantSettingService.readAll()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(toResources(dynamicTenantSettingService.readAll(names)), HttpStatus.OK);
        }
    }

    @Override
    public EntityModel<DynamicTenantSetting> toResource(DynamicTenantSetting element, Object... extras) {
        //TODO add link
        return new EntityModel<>(element);
    }
}
