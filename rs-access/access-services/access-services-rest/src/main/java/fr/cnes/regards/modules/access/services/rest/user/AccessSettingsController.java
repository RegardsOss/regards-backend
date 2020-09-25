/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.accessrights.client.IAccessSettingsClient;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.database.DefaultDownloadQuotaLimits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.StringJoiner;

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
        ResponseEntity<EntityModel<AccessSettings>> accessResponse =
            accessSettingsClient.retrieveAccessSettings();

        ResponseEntity<DefaultDownloadQuotaLimits> quotasResponse =
            storageClient.getDefaultDownloadQuotaLimits();

        boolean accessRequestFailed = !accessResponse.getStatusCode().is2xxSuccessful();
        boolean quotasRequestFailed = !quotasResponse.getStatusCode().is2xxSuccessful();
        if (accessRequestFailed || quotasRequestFailed) {
            StringJoiner s = new StringJoiner("\n");
            if (accessRequestFailed) { s.add(String.format("Request to accessrights-client failed with %s.", accessResponse.getStatusCodeValue())); }
            if (quotasRequestFailed) { s.add(String.format("Request to storage-client failed with %s.", quotasResponse.getStatusCodeValue())); }
            LOGGER.error(s.toString());
            throw new ModuleException("Unable to request access settings.");
        }

        return new ResponseEntity<>(
                toResource(new AccessSettingsDto(
                    accessResponse.getBody().getContent().getId(),
                    accessResponse.getBody().getContent().getMode(),
                    quotasResponse.getBody().getMaxQuota(),
                    quotasResponse.getBody().getRateLimit()
                )),
                HttpStatus.OK
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
        AccessSettings accessSettings = new AccessSettings();
        accessSettings.setId(accessSettingsDto.getId());
        accessSettings.setMode(accessSettingsDto.getMode());
        ResponseEntity<EntityModel<AccessSettings>> accessResponse =
            accessSettingsClient.updateAccessSettings(accessSettings);

        DefaultDownloadQuotaLimits defaultLimits =
            new DefaultDownloadQuotaLimits(accessSettingsDto.getMaxQuota(), accessSettingsDto.getRateLimit());
        ResponseEntity<DefaultDownloadQuotaLimits> quotasResponse =
            storageClient.changeDefaultDownloadQuotaLimits(defaultLimits);

        boolean accessRequestFailed = !accessResponse.getStatusCode().is2xxSuccessful();
        boolean quotasRequestFailed = !quotasResponse.getStatusCode().is2xxSuccessful();
        if (accessRequestFailed || quotasRequestFailed) {
            if (accessRequestFailed) { JOINER.add(String.format("Request to accessrights-client failed with %s.", accessResponse.getStatusCodeValue())); }
            if (quotasRequestFailed) { JOINER.add(String.format("Request to storage-client failed with %s.", quotasResponse.getStatusCodeValue())); }
            LOGGER.error(JOINER.toString());
            throw new ModuleException("Unable to update access settings.");
        }

        return new ResponseEntity<>(
            toResource(accessSettingsDto),
            HttpStatus.OK
        );
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
