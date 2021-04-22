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
package fr.cnes.regards.modules.accessrights.instance.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSettings;
import fr.cnes.regards.modules.accessrights.instance.service.IAccountSettingsService;

/**
 * Class AccountSettingsController
 *
 * REST Controller to manage accounts global settings
 * @author Sébastien Binda

 */
@RestController
@RequestMapping(path = AccountSettingsController.REQUEST_MAPPING_ROOT)
public class AccountSettingsController implements IResourceController<AccountSettings> {

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String REQUEST_MAPPING_ROOT = "/accounts/settings";

    /**
     * Business service to manage accountsSettings
     */
    @Autowired
    private IAccountSettingsService accountSettingsService;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Retrieve the {@link AccountSettings} for the instance.
     * @return The {@link AccountSettings} wrapped in a {@link EntityModel} and a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the list of setting managing the accounts",
            role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<AccountSettings>> retrieveAccountSettings() {
        AccountSettings settings = accountSettingsService.retrieve();
        return new ResponseEntity<>(toResource(settings), HttpStatus.OK);
    }

    /**
     * Update the {@link AccountSettings} for the instance.
     * @param updatedAccountSetting The {@link AccountSettings}
     * @return The updated {@link AccountSettings}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "update the setting managing the account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<AccountSettings>> updateAccountSetting(
            @Valid @RequestBody AccountSettings updatedAccountSetting) throws EntityNotFoundException {
        AccountSettings updated = accountSettingsService.update(updatedAccountSetting);
        return new ResponseEntity<>(toResource(updated), HttpStatus.OK);
    }

    @Override
    public EntityModel<AccountSettings> toResource(AccountSettings element, final Object... extras) {
        EntityModel<AccountSettings> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveAccountSettings", LinkRels.SELF);
        resourceService.addLink(resource, this.getClass(), "updateAccountSetting", LinkRels.UPDATE,
                                MethodParamFactory.build(AccountSettings.class));

        return resource;
    }

}
