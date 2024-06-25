/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.fileaccess.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginConfigurationDto;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.fileaccess.domain.StorageLocationConfiguration;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationConfigurationDto;
import fr.cnes.regards.modules.fileaccess.service.StorageLocationConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/**
 * REST controller managing CRUD operations on {@link StorageLocationConfiguration}s.
 *
 * @author Iliana Ghazali
 */
@RestController
@RequestMapping(StorageLocationConfigurationController.BASE_PATH)
public class StorageLocationConfigurationController implements IResourceController<StorageLocationConfigurationDto> {

    // PATHS

    public static final String BASE_PATH = "/storages";

    public static final String ID_PATH = "/{id}";

    // SERVICES

    private final StorageLocationConfigurationService storageLocationConfigService;

    private final IResourceService resourceService;

    public StorageLocationConfigurationController(StorageLocationConfigurationService storageLocationConfigService,
                                                  IResourceService resourceService) {
        this.storageLocationConfigService = storageLocationConfigService;
        this.resourceService = resourceService;
    }

    // GET ENDPOINTS

    @Operation(summary = "Retrieve all storage location configurations.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Existing storage location configurations."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.") })
    @GetMapping
    @ResourceAccess(description = "Retrieve all storage location configurations.", role = DefaultRole.EXPLOIT)
    public ResponseEntity<List<EntityModel<StorageLocationConfigurationDto>>> retrieveAllStorageLocationConfigs() {
        return ResponseEntity.ok(toResources(storageLocationConfigService.searchAll()
                                                                         .stream()
                                                                         .map(StorageLocationConfiguration::toDto)
                                                                         .sorted(Comparator.comparing(
                                                                             StorageLocationConfigurationDto::getName))
                                                                         .toList()));
    }

    @Operation(summary = "Retrieve an existing storage location configuration by unique name.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "The requested storage location configuration."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user."),
                            @ApiResponse(responseCode = "404",
                                         description = "The requested storage location configuration was not found.") })
    @GetMapping(ID_PATH)
    @ResourceAccess(description = "Retrieve an existing storage location configuration by name.",
                    role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<StorageLocationConfigurationDto>> retrieveStorageLocationConfigByName(
        @PathVariable(name = "id") String storageName) {
        return storageLocationConfigService.search(storageName)
                                           .map(s -> ResponseEntity.ok(toResource(s.toDto())))
                                           .orElse(ResponseEntity.notFound().build());
    }
    // POST/PUT ENDPOINTS

    @Operation(summary = "Create a new storage location configuration.")
    @ApiResponses(value = { @ApiResponse(responseCode = "201",
                                         description = "The storage location configuration was successfully created."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user."),
                            @ApiResponse(responseCode = "422", description = "The request is malformed.") })
    @PostMapping
    @ResourceAccess(description = "Create a new storage location configuration.", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<StorageLocationConfigurationDto>> createStorageLocationConfig(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Storage location configuration to "
                                                                            + "create.",
                                                              content = @Content(schema = @Schema(implementation = StorageLocationConfigurationDto.class)))
        @Valid @RequestBody StorageLocationConfigurationDto storageLocationConfigDto) throws ModuleException {
        PluginConfigurationDto pluginConfigDtoProvided = storageLocationConfigDto.getPluginConfiguration();
        return new ResponseEntity<>(toResource(storageLocationConfigService.create(storageLocationConfigDto.getName(),
                                                                                   pluginConfigDtoProvided != null ?
                                                                                       PluginConfiguration.fromDto(
                                                                                           pluginConfigDtoProvided) :
                                                                                       null,
                                                                                   storageLocationConfigDto.getAllocatedSizeInKo())
                                                                           .toDto()), HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing storage location configuration by unique name.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "The storage location configuration was successfully updated."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user."),
                            @ApiResponse(responseCode = "404",
                                         description = "The requested storage location configuration was not found."),
                            @ApiResponse(responseCode = "422", description = "The request is malformed.") })
    @PutMapping(ID_PATH)
    @ResourceAccess(description = "Update an existing storage location configuration.", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<StorageLocationConfigurationDto>> updateStorageLocationConfigByName(
        @PathVariable(name = "id") String storageName,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Storage location configuration to be "
                                                                            + "updated.",
                                                              content = @Content(schema = @Schema(implementation = StorageLocationConfigurationDto.class)))
        @Valid @RequestBody StorageLocationConfigurationDto storageLocationConfigDto) throws ModuleException {
        return ResponseEntity.ok(toResource(storageLocationConfigService.update(storageName,
                                                                                StorageLocationConfiguration.fromDto(
                                                                                    storageLocationConfigDto))
                                                                        .toDto()));
    }

    // DELETE ENDPOINTS

    @Operation(summary = "Delete an existing storage location configuration dto by unique name.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "The requested storage location configuration was successfully "
                                                       + "deleted."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user."),
                            @ApiResponse(responseCode = "404",
                                         description = "The requested storage location configuration was not found.") })
    @DeleteMapping(ID_PATH)
    @ResourceAccess(description = "Delete an existing storage location configuration by name.",
                    role = DefaultRole.ADMIN)
    public ResponseEntity<Void> deleteStorageLocationConfigByName(@PathVariable(name = "id") String storageName)
        throws ModuleException {
        storageLocationConfigService.delete(storageName);
        return ResponseEntity.ok().build();
    }

    // HATEOAS LINKS

    @Override
    public EntityModel<StorageLocationConfigurationDto> toResource(StorageLocationConfigurationDto storageLocationConfigDto,
                                                                   Object... extras) {
        EntityModel<StorageLocationConfigurationDto> resource = EntityModel.of(storageLocationConfigDto);
        String storageName = storageLocationConfigDto.getName();
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrieveStorageLocationConfigByName",
                                LinkRels.SELF,
                                MethodParamFactory.build(String.class, storageName));
        resourceService.addLink(resource,
                                this.getClass(),
                                "updateStorageLocationConfigByName",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, storageName),
                                MethodParamFactory.build(StorageLocationConfigurationDto.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "deleteStorageLocationConfigByName",
                                LinkRels.DELETE,
                                MethodParamFactory.build(String.class, storageName));
        return resource;
    }

}