/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.client.IUserResourceClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Primary
@Component
public class UserResourceClientMock implements IUserResourceClient, IResourceController<ResourcesAccess> {

    public static final String REQUEST_ACCESS_STUB_CONTROLLER_SIMPLE_NAME = "controllerSimpleName";
    public static final String REQUEST_ACCESS_STUB_RESOURCE = "resource";
    public static final String REQUEST_ACCESS_STUB_MICROSERVICE = "microservice";
    public static final String REQUEST_ACCESS_STUB_DESCRIPTION = "description";
    public static final long REQUEST_ACCESS_STUB_ID = new Random().nextInt(10_000);
    public static final ResourcesAccess RESOURCES_ACCESS_STUB = new ResourcesAccess(
        REQUEST_ACCESS_STUB_ID,
        REQUEST_ACCESS_STUB_DESCRIPTION,
        REQUEST_ACCESS_STUB_MICROSERVICE,
        REQUEST_ACCESS_STUB_RESOURCE,
        REQUEST_ACCESS_STUB_CONTROLLER_SIMPLE_NAME,
        RequestMethod.OPTIONS,
        DefaultRole.REGISTERED_USER
    );

    @Autowired
    private IResourceService resourceService;


    @Override
    public ResponseEntity<List<EntityModel<ResourcesAccess>>> retrieveProjectUserResources(String pUserLogin, String pBorrowedRoleName) {
        return new ResponseEntity<>(
            toResources(
                Collections.singleton(RESOURCES_ACCESS_STUB),
                pUserLogin
            ),
            HttpStatus.OK
        );
    }

    @Override
    public ResponseEntity<Void> updateProjectUserResources(String pLogin, @Valid List<ResourcesAccess> pUpdatedUserAccessRights) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> removeProjectUserResources(String pUserLogin) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<ResourcesAccess> toResource(ResourcesAccess element, Object... extras) {
        return resourceService.toResource(element);
    }
}
