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

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.access.services.domain.user.AccessSettingsDto;
import fr.cnes.regards.modules.access.services.rest.user.utils.ComposableClientException;
import fr.cnes.regards.modules.accessrights.client.IAccessSettingsClient;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.database.DefaultDownloadQuotaLimits;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.StringJoiner;

import static fr.cnes.regards.modules.access.services.rest.user.utils.Try.handleClientFailure;

/**
 * Class AccountSettingsController
 *
 * REST Controller to manage access global settings. Accesses are the state of project users during the activation
 * process
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(path = AccessSettingsController.REQUEST_MAPPING_ROOT)
public class AccessSettingsController implements IResourceController<AccessSettingsDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessSettingsController.class);

    private static final StringJoiner JOINER = new StringJoiner("\n");

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String REQUEST_MAPPING_ROOT = "/accesses/settings";

    /**
     * Client handling CRUD operation on {@link AccessSettings}. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IAccessSettingsClient accessSettingsClient;

    /**
     * Client handling storage quotas
     */
    @Autowired
    private IStorageRestClient storageClient;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Retrieve the {@link AccessSettingsDto}.
     * @return The {@link AccessSettingsDto}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieves the settings managing the access requests", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<AccessSettingsDto>> retrieveAccessSettings() throws ModuleException {
        return toResponse(
            Validation
                .combine(
                    Try.of(() -> accessSettingsClient.retrieveAccessSettings())
                        .transform(handleClientFailure("accessrights-client"))
                        .map(EntityModel::getContent),
                    Try.of(() -> storageClient.getDefaultDownloadQuotaLimits())
                        .map(ResponseEntity::getBody)
                        // special value for frontend if any error on storage or storage not deploy
                        .onFailure(t -> LOGGER.debug("Failed to query rs-storage for quotas.", t))
                        .orElse(() -> Try.success(new DefaultDownloadQuotaLimits(null, null)))
                        .toValidation(ComposableClientException::make)
                )
                .ap((accessSettings, defaultLimits) -> new AccessSettingsDto(
                    accessSettings.getId(),
                    accessSettings.getMode(),
                    accessSettings.getDefaultRole(),
                    accessSettings.getDefaultGroups(),
                    defaultLimits.getMaxQuota(),
                    defaultLimits.getRateLimit()
                ))
                .mapError(s -> new ModuleException(s.reduce(ComposableClientException::compose)))
                .map(this::toResource)
                .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
        );
    }

    /**
     * Update the {@link AccessSettings}.
     * @param accessSettingsDto The {@link AccessSettingsDto}
     * @return The updated access settings
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "Updates the setting managing the access requests", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<AccessSettingsDto>> updateAccessSettings(@Valid @RequestBody AccessSettingsDto accessSettingsDto) throws ModuleException {
        return toResponse(
            Validation
                .combine(
                    Try.of(() -> {
                        AccessSettings accessSettings = new AccessSettings();
                        accessSettings.setId(accessSettingsDto.getId());
                        accessSettings.setMode(accessSettingsDto.getMode());
                        accessSettings.setDefaultRole(accessSettingsDto.getRole());
                        accessSettings.setDefaultGroups(accessSettingsDto.getGroups());
                        return accessSettings;
                    })
                        .map(accessSettingsClient::updateAccessSettings)
                        .transform(handleClientFailure("accessrights-client"))
                        .map(EntityModel::getContent),
                    Try.of(() -> new DefaultDownloadQuotaLimits(accessSettingsDto.getMaxQuota(), accessSettingsDto.getRateLimit()))
                        .map(storageClient::changeDefaultDownloadQuotaLimits)
                        .map(ResponseEntity::getBody)
                        // special value for frontend if any error on storage or storage not deploy
                        .onFailure(t -> LOGGER.debug("Failed to query rs-storage for quotas.", t))
                        .orElse(() -> Try.success(new DefaultDownloadQuotaLimits(null, null)))
                        .toValidation(ComposableClientException::make)
                )
                .ap((accessSettings, defaultLimits) -> new AccessSettingsDto(
                    accessSettings.getId(),
                    accessSettings.getMode(),
                    accessSettings.getDefaultRole(),
                    accessSettings.getDefaultGroups(),
                    defaultLimits.getMaxQuota(),
                    defaultLimits.getRateLimit()
                ))
                .mapError(s -> new ModuleException(s.reduce(ComposableClientException::compose)))
                .map(this::toResource)
                .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
        );
    }

    private <V> V toResponse(
        Validation<ModuleException, V> v
    ) throws ModuleException {
        if (v.isValid()) {
            return v.get();
        } else {
            throw v.getError();
        }
    }

    @Override
    public EntityModel<AccessSettingsDto> toResource(final AccessSettingsDto element, final Object... extras) {
        EntityModel<AccessSettingsDto> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveAccessSettings", LinkRels.SELF);
        resourceService.addLink(resource, this.getClass(), "updateAccessSettings", LinkRels.UPDATE,
                                MethodParamFactory.build(AccessSettingsDto.class));

        return resource;
    }

}
