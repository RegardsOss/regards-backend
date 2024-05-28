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
package fr.cnes.regards.modules.dam.rest.dataaccess;

import fr.cnes.regards.framework.hateoas.*;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroupMapper;
import fr.cnes.regards.modules.dam.dto.AccessGroupDto;
import fr.cnes.regards.modules.dam.service.dataaccess.IAccessGroupService;
import fr.cnes.regards.modules.dam.service.dataaccess.IAccessRightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AccessGroupController.PATH_ACCESS_GROUPS)
public class AccessGroupController implements IResourceController<AccessGroupDto> {

    public static final String PATH_ACCESS_GROUPS = "/accessgroups";

    public static final String PATH_ACCESS_GROUPS_NAME = "/{name}";

    private final IResourceService resourceService;

    private final IAccessGroupService accessGroupService;

    private final IAccessRightService accessRightService;

    private final AccessGroupMapper accessGroupMapper;

    public AccessGroupController(IResourceService resourceService,
                                 IAccessGroupService accessGroupService,
                                 IAccessRightService accessRightService,
                                 AccessGroupMapper accessGroupMapper) {
        this.resourceService = resourceService;
        this.accessGroupService = accessGroupService;
        this.accessRightService = accessRightService;
        this.accessGroupMapper = accessGroupMapper;
    }

    @GetMapping
    @Operation(summary = "Get groups of user", description = "Return a page of groups of user")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "All groups of user were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve all groups of user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<AccessGroupDto>>> retrieveAccessGroupsList(
        @RequestParam(name = "public", required = false) boolean isPublic,
        @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<AccessGroupDto> assembler) {

        Page<AccessGroup> accessGroupPage = accessGroupService.retrieveAccessGroups(isPublic, pageable);

        return ResponseEntity.ok(toPagedResources(new PageImpl<>(accessGroupPage.stream()
                                                                                .map(accessGroupMapper::convertToAccessGroupDto)
                                                                                .toList(),
                                                                 accessGroupPage.getPageable(),
                                                                 accessGroupPage.getTotalElements()), assembler));
    }

    @PostMapping
    @ResourceAccess(description = "create an access group according to the parameter")
    public ResponseEntity<EntityModel<AccessGroupDto>> createAccessGroup(@Valid @RequestBody AccessGroupDto toBeCreated)
        throws EntityAlreadyExistsException {

        AccessGroup accessGroupToBeCreated = accessGroupMapper.convertToAccessGroup(toBeCreated);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(toResource(accessGroupMapper.convertToAccessGroupDto(accessGroupService.createAccessGroup(
                                 accessGroupToBeCreated))));
    }

    @GetMapping(PATH_ACCESS_GROUPS_NAME)
    @ResourceAccess(description = "send the access group of name requested", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<AccessGroupDto>> retrieveAccessGroup(@Valid @PathVariable("name")
                                                                           String groupName)
        throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(accessGroupMapper.convertToAccessGroupDto(accessGroupService.retrieveAccessGroup(
            groupName))));
    }

    @DeleteMapping(PATH_ACCESS_GROUPS_NAME)
    @ResourceAccess(description = "delete the access group of name requested")
    public ResponseEntity<Void> deleteAccessGroup(@Valid @PathVariable("name") String groupName)
        throws EntityOperationForbiddenException, EntityNotFoundException {
        accessGroupService.deleteAccessGroup(groupName);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(PATH_ACCESS_GROUPS_NAME)
    @ResourceAccess(description = "only used to modify the privacy of the group")
    public ResponseEntity<EntityModel<AccessGroupDto>> updateAccessGroup(@Valid @PathVariable("name") String groupName,
                                                                         @Valid @RequestBody
                                                                         AccessGroupDto accessGroupDto)
        throws ModuleException {
        return ResponseEntity.ok(toResource(accessGroupMapper.convertToAccessGroupDto(accessGroupService.update(
            groupName,
            accessGroupMapper.convertToAccessGroup(accessGroupDto)))));
    }

    @Override
    public EntityModel<AccessGroupDto> toResource(AccessGroupDto accessGroupDto, Object... pExtras) {
        EntityModel<AccessGroupDto> resource = resourceService.toResource(accessGroupDto);
        MethodParam<String> nameParam = MethodParamFactory.build(String.class, accessGroupDto.getName());
        resourceService.addLink(resource, this.getClass(), "retrieveAccessGroup", LinkRels.SELF, nameParam);
        resourceService.addLink(resource,
                                this.getClass(),
                                "updateAccessGroup",
                                LinkRels.UPDATE,
                                nameParam,
                                MethodParamFactory.build(AccessGroupDto.class));
        if (!accessRightService.hasAccessRights(accessGroupMapper.convertToAccessGroup(accessGroupDto))) {
            resourceService.addLink(resource, this.getClass(), "deleteAccessGroup", LinkRels.DELETE, nameParam);
        }
        resourceService.addLink(resource,
                                this.getClass(),
                                "createAccessGroup",
                                LinkRels.CREATE,
                                MethodParamFactory.build(AccessGroupDto.class, accessGroupDto));
        return resource;
    }

}
