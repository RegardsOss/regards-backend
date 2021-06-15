/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.access.services.rest.user;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSettingDto;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.modules.access.services.rest.user.utils.ComposableClientException;
import static fr.cnes.regards.modules.access.services.rest.user.utils.Try.handleClientFailure;
import fr.cnes.regards.modules.accessrights.client.IAccessRightSettingClient;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import io.vavr.control.Try;
import io.vavr.control.Validation;

/**
 * Class AccountSettingsController
 *
 * REST Controller to manage access global settings. Accesses are the state of project users during the activation
 * process
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(path = AccessSettingsController.REQUEST_MAPPING_ROOT)
public class AccessSettingsController implements IResourceController<DynamicTenantSettingDto> {

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String REQUEST_MAPPING_ROOT = "/accesses/settings";

    public static final String NAME_PATH = "/{name}";

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessSettingsController.class);

    private static final Set<String> STORAGE_PARAMETER_NAMES = Sets
            .newHashSet(StorageSetting.MAX_QUOTA_NAME, StorageSetting.RATE_LIMIT_NAME);

    /**
     * Client handling CRUD operation on {@link AccessSettings}. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IAccessRightSettingClient accessSettingsClient;

    /**
     * Client handling storage quotas
     */
    @Autowired
    private IStorageSettingClient storageSettingClient;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAuthenticationResolver authentivationResolver;

    @Value("${spring.application.name}")
    private String appName;

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieves the settings managing the access requests",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<EntityModel<DynamicTenantSettingDto>>> retrieveAccessSettings() throws ModuleException {
        return toResponse(Validation.combine(Try.run(() -> FeignSecurityManager
                                                     .asUser(authentivationResolver.getUser(), RoleAuthority.getSysRole(appName)))
                                                     .map(unused -> accessSettingsClient.retrieveAll())
                                                     .andFinally(FeignSecurityManager::reset)
                                                     .transform(handleClientFailure("accessrights-client"))
                                                     .map(HateoasUtils::unwrapCollection),
                                             Try.run(() -> FeignSecurityManager.asUser(authentivationResolver.getUser(),
                                                                                       RoleAuthority
                                                                                               .getSysRole(appName)))
                                                     .map(unused -> storageSettingClient
                                                             .retrieveAll(STORAGE_PARAMETER_NAMES))
                                                     .andFinally(FeignSecurityManager::reset)
                                                     .map(ResponseEntity::getBody).map(HateoasUtils::unwrapCollection)
                                                     // special value for frontend if any error on storage or storage not deploy
                                                     .onFailure(t -> LOGGER
                                                             .debug("Failed to query rs-storage for quotas.", t))
                                                     .orElse(() -> Try.success(new ArrayList<>()))
                                                     .toValidation(ComposableClientException::make))
                                  .ap((accessSettings, defaultLimits) -> {
                                      List<DynamicTenantSettingDto> result = new ArrayList<>(accessSettings);
                                      result.addAll(defaultLimits);
                                      return result;
                                  }).mapError(s -> new ModuleException(s.reduce(ComposableClientException::compose)))
                                  .map(this::toResources).map(dto -> new ResponseEntity<>(dto, HttpStatus.OK)));
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.PUT, path = NAME_PATH)
    @ResourceAccess(description = "Updates the setting managing the access requests", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<DynamicTenantSettingDto>> updateAccessSettings(
            @PathVariable(name = "name") String name,
            @Valid @RequestBody DynamicTenantSettingDto dynamicTenantSettingDto) {
        if (STORAGE_PARAMETER_NAMES.contains(name)) {
            ResponseEntity<EntityModel<DynamicTenantSettingDto>> storageResponse = storageSettingClient
                    .update(name, dynamicTenantSettingDto);
            return new ResponseEntity<>(toResource(storageResponse.getBody().getContent()),
                                        storageResponse.getStatusCode());
        } else {
            ResponseEntity<EntityModel<DynamicTenantSettingDto>> adminResponse = accessSettingsClient
                    .update(dynamicTenantSettingDto.getName(), dynamicTenantSettingDto);
            return new ResponseEntity<>(toResource(adminResponse.getBody().getContent()),
                                        adminResponse.getStatusCode());
        }
    }

    private <V> V toResponse(Validation<ModuleException, V> v) throws ModuleException {
        if (v.isValid()) {
            return v.get();
        } else {
            throw v.getError();
        }
    }

    @Override
    public EntityModel<DynamicTenantSettingDto> toResource(final DynamicTenantSettingDto element,
            final Object... extras) {
        EntityModel<DynamicTenantSettingDto> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveAccessSettings", LinkRels.SELF);
        resourceService.addLink(resource,
                                this.getClass(),
                                "updateAccessSettings",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, element.getName()),
                                MethodParamFactory.build(DynamicTenantSettingDto.class));

        return resource;
    }

}
