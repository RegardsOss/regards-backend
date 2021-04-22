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
package fr.cnes.regards.modules.access.services.rest.user.mock;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.access.services.rest.user.AccessSettingsController;
import fr.cnes.regards.modules.accessrights.client.IAccessSettingsClient;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Primary
@Component
public class AccessSettingsClientMock implements IAccessSettingsClient, IResourceController<AccessSettings> {

    public static final long ACCESS_SETTINGS_STUB_ID = new Random().nextInt(10_000);
    public static final String ACCESS_SETTINGS_STUB_MODE = AccessSettings.AUTO_ACCEPT_MODE;
    public static final Role ACCESS_SETTINGS_STUB_ROLE = new Role(DefaultRole.REGISTERED_USER.toString());
    public static final List<String> ACCESS_SETTINGS_STUB_GROUPS = Lists.newArrayList("dummy");
    public static final AccessSettings ACCESS_SETTINGS_STUB;
    static {
        ACCESS_SETTINGS_STUB = new AccessSettings();
        ACCESS_SETTINGS_STUB.setId(ACCESS_SETTINGS_STUB_ID);
        ACCESS_SETTINGS_STUB.setMode(ACCESS_SETTINGS_STUB_MODE);
        ACCESS_SETTINGS_STUB.setDefaultRole(ACCESS_SETTINGS_STUB_ROLE);
        ACCESS_SETTINGS_STUB.setDefaultGroups(ACCESS_SETTINGS_STUB_GROUPS);
    }

    @Autowired
    private IResourceService resourceService;

    @Override
    public ResponseEntity<EntityModel<AccessSettings>> retrieveAccessSettings() {
        return new ResponseEntity<>(toResource(ACCESS_SETTINGS_STUB), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EntityModel<AccessSettings>> updateAccessSettings(AccessSettings accessSettings) {
        return new ResponseEntity<>(toResource(accessSettings), HttpStatus.OK);
    }

    @Override
    public EntityModel<AccessSettings> toResource(final AccessSettings element, final Object... extras) {
        EntityModel<AccessSettings> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveAccessSettings", LinkRels.SELF);
        resourceService.addLink(resource, this.getClass(), "updateAccessSettings", LinkRels.UPDATE,
            MethodParamFactory.build(AccessSettings.class));

        return resource;
    }
}
