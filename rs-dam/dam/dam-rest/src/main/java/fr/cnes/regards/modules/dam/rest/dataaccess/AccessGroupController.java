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
package fr.cnes.regards.modules.dam.rest.dataaccess;

import fr.cnes.regards.framework.hateoas.*;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dam.service.dataaccess.IAccessGroupService;
import fr.cnes.regards.modules.dam.service.dataaccess.IAccessRightService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


@RestController
@RequestMapping(AccessGroupController.PATH_ACCESS_GROUPS)
public class AccessGroupController implements IResourceController<AccessGroup> {

    public static final String PATH_ACCESS_GROUPS = "/accessgroups";
    public static final String PATH_ACCESS_GROUPS_NAME = "/{name}";

    private final IResourceService resourceService;
    private final IAccessGroupService accessGroupService;
    private final IAccessRightService accessRightService;

    public AccessGroupController(IResourceService resourceService, IAccessGroupService accessGroupService, IAccessRightService accessRightService) {
        this.resourceService = resourceService;
        this.accessGroupService = accessGroupService;
        this.accessRightService = accessRightService;
    }

    @GetMapping
    @ResourceAccess(description = "send the whole list of accessGroups", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<AccessGroup>>> retrieveAccessGroupsList(
            @RequestParam(name = "public", required = false) Boolean isPublic,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<AccessGroup> assembler
    ) {
        return ResponseEntity.ok(toPagedResources(accessGroupService.retrieveAccessGroups(isPublic, pageable), assembler));
    }

    @PostMapping
    @ResourceAccess(description = "create an access group according to the parameter")
    public ResponseEntity<EntityModel<AccessGroup>> createAccessGroup(@Valid @RequestBody AccessGroup toBeCreated) throws EntityAlreadyExistsException {
        return ResponseEntity.status(HttpStatus.CREATED).body(toResource(accessGroupService.createAccessGroup(toBeCreated)));
    }

    @GetMapping(PATH_ACCESS_GROUPS_NAME)
    @ResourceAccess(description = "send the access group of name requested", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<AccessGroup>> retrieveAccessGroup(@Valid @PathVariable("name") String groupName) throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(accessGroupService.retrieveAccessGroup(groupName)));
    }

    @DeleteMapping(PATH_ACCESS_GROUPS_NAME)
    @ResourceAccess(description = "delete the access group of name requested")
    public ResponseEntity<Void> deleteAccessGroup(@Valid @PathVariable("name") String groupName) throws EntityOperationForbiddenException, EntityNotFoundException {
        accessGroupService.deleteAccessGroup(groupName);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(PATH_ACCESS_GROUPS_NAME)
    @ResourceAccess(description = "only used to modify the privacy of the group")
    public ResponseEntity<EntityModel<AccessGroup>> updateAccessGroup(@Valid @PathVariable("name") String groupName, @Valid @RequestBody AccessGroup accessGroup)
            throws ModuleException {
        return ResponseEntity.ok(toResource(accessGroupService.update(groupName, accessGroup)));
    }

    @Override
    public EntityModel<AccessGroup> toResource(AccessGroup accessGroup, Object... pExtras) {
        EntityModel<AccessGroup> resource = resourceService.toResource(accessGroup);
        MethodParam<String> nameParam = MethodParamFactory.build(String.class, accessGroup.getName());
        resourceService.addLink(resource, this.getClass(), "retrieveAccessGroup", LinkRels.SELF, nameParam);
        resourceService.addLink(resource, this.getClass(), "updateAccessGroup", LinkRels.UPDATE, nameParam, MethodParamFactory.build(AccessGroup.class));
        if (!accessRightService.hasAccessRights(accessGroup)) {
            resourceService.addLink(resource, this.getClass(), "deleteAccessGroup", LinkRels.DELETE, nameParam);
        }
        resourceService.addLink(resource, this.getClass(), "createAccessGroup", LinkRels.CREATE, MethodParamFactory.build(AccessGroup.class, accessGroup));
        return resource;
    }

}
