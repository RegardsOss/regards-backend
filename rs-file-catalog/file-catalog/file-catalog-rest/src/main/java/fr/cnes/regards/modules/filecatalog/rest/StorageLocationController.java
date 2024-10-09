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
package fr.cnes.regards.modules.filecatalog.rest;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.StorageRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import fr.cnes.regards.modules.filecatalog.service.location.StorageLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller to access REST Actions on storage locations.
 *
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(StorageLocationController.BASE_PATH)
public class StorageLocationController implements IResourceController<StorageLocationDto> {

    public static final String BASE_PATH = "/storages";

    public static final String RUN_MONITORING = "/monitoring/run";

    public static final String FILES = "/files";

    public static final String ID_PATH = "/{id}";

    public static final String RETRY = "/retry/{type}";

    public static final String RETRY_SESSION = "/retry/{source}/{session}";

    public static final String RUN_PERIODIC_ACTION_PATH = "/periodic-actions/run";

    public static final String RESET_PARAM = "reset";

    public static final String METHOD_DELETE_FILES = "deleteFiles";

    public static final String METHOD_UPDATE_LOCATION_CONFIGURATION = "updateLocationConfiguration";

    public static final String METHOD_CONFIGURE_LOCATION = "configureLocation";

    public static final String METHOD_DELETE = "delete";

    private static final String REQUESTS_PATH = "/requests/{type}";

    private final IResourceService resourceService;

    private final IAuthenticationResolver authenticationResolver;

    private final StorageLocationService storageLocationService;

    public StorageLocationController(IResourceService resourceService,
                                     IAuthenticationResolver authenticationResolver,
                                     StorageLocationService storageLocationService) {
        this.resourceService = resourceService;
        this.authenticationResolver = authenticationResolver;
        this.storageLocationService = storageLocationService;
    }

    /**
     * End-point to create a new storage location
     *
     * @param storageLocation storage location name
     * @return {@link StorageLocationDto}
     * @throws ModuleException if location does not exists
     */
    @PostMapping
    @ResourceAccess(description = "Configure a storage location by his name", role = DefaultRole.ADMIN)
    @Operation(summary = "Create a new storage location",
               description = "Create a new storage location by specifying its name. Returns the created storage location.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Storage location created successfully.") })
    public ResponseEntity<EntityModel<StorageLocationDto>> configureLocation(
        @Valid @RequestBody StorageLocationDto storageLocation) throws ModuleException {
        return new ResponseEntity<>(toResource(storageLocationService.createStorageLocation(storageLocation)),
                                    HttpStatus.CREATED);
    }

    @PostMapping(path = RUN_PERIODIC_ACTION_PATH)
    @Operation(summary = "Force running of periodic tasks",
               description = "Trigger the execution of periodic tasks on all storage locations.")
    @ApiResponse(responseCode = "200", description = "Periodic tasks were triggered successfully.")
    @ResourceAccess(description = "Force running of periodic tasks on storage locations",
                    role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> runPeriodicTasks() {
        // FIXME call file-packager controller once its implemented
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to update a storage location configuration.
     *
     * @param storageLocation to update
     * @return updated {@link StorageLocationDto}
     * @throws ModuleException if location does not exists
     */
    @PutMapping(path = ID_PATH)
    @Operation(summary = "Update a storage location configuration",
               description = "Update the configuration of a storage location by providing its details.")
    @ApiResponse(responseCode = "200", description = "Storage location configuration updated successfully.")
    @ResourceAccess(description = "Update a storage location configuration", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<StorageLocationDto>> updateLocationConfiguration(
        @PathVariable(name = "id") String storageName, @Valid @RequestBody StorageLocationDto storageLocation)
        throws ModuleException {
        return new ResponseEntity<>(toResource(storageLocationService.updateLocationConfiguration(storageName,
                                                                                                  storageLocation)),
                                    HttpStatus.OK);
    }

    /**
     * End-point to retrieve all known storage locations
     *
     * @return {@link StorageLocationDto}s
     */
    @GetMapping
    @Operation(summary = "Get known storage locations", description = "Return a list of known storage locations")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "All known storage locations were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve list of all known storage locations.",
                    role = DefaultRole.EXPLOIT)
    public ResponseEntity<List<EntityModel<StorageLocationDto>>> retrieve() throws ModuleException {
        return new ResponseEntity<>(toResources(storageLocationService.findAllStorageLocations()), HttpStatus.OK);
    }

    /**
     * End-point to retrieve a Storage location by its name
     *
     * @param storageName storage location name
     */
    @GetMapping(path = ID_PATH)
    @Operation(summary = "Retrieve a storage location by name",
               description = "Fetch a storage location's details by specifying its name.")
    @ApiResponse(responseCode = "200", description = "Storage location retrieved successfully.")
    @ResourceAccess(description = "Retrieve a storage location by its name", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<StorageLocationDto>> retrieve(@PathVariable(name = "id") String storageName)
        throws ModuleException {
        return new ResponseEntity<>(toResource(storageLocationService.findStorageLocationByName(storageName)),
                                    HttpStatus.OK);
    }

    /**
     * End-point to delete a storage location and its configuration
     *
     * @param storageName storage location name to delete
     */
    @DeleteMapping(path = ID_PATH)
    @Operation(summary = "Delete a storage location",
               description = "Delete a storage location and its configuration by specifying its name.")
    @ApiResponse(responseCode = "200", description = "Storage location deleted successfully.")
    @ResourceAccess(description = "Delete storage location", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable(name = "id") String storageName) throws ModuleException {
        storageLocationService.delete(storageName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to delete the requests of a storage location
     *
     * @param storageName storage location name to delete
     */
    @DeleteMapping(path = ID_PATH + REQUESTS_PATH)
    @Operation(summary = "Delete storage requests",
               description = "Delete requests of a storage location based on its name, request type, and optionally the request status.")
    @ApiResponse(responseCode = "200", description = "Storage requests deleted successfully.")
    @ResourceAccess(description = "Delete storage requests", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> deleteRequests(@PathVariable(name = "id") String storageName,
                                               @PathVariable(name = "type") FileRequestType type,
                                               @RequestParam(name = "status", required = false)
                                               StorageRequestStatus status) throws ModuleException {
        storageLocationService.deleteRequests(storageName, type, Optional.ofNullable(status));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to delete all files referenced in a storage location
     *
     * @param storageName storage location name
     * @param forceDelete If true, files are unreferenced even if the physical files cannot be deleted.
     */
    @DeleteMapping(path = ID_PATH + FILES)
    @Operation(summary = "Delete all files in a storage location",
               description = "Delete all files referenced in the specified storage location. Optionally, use force delete to unreference files even if physical deletion fails.")
    @ApiResponse(responseCode = "200", description = "All files in the storage location deleted successfully.")
    @ResourceAccess(description = "Delete all files of the storage location", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> deleteFiles(@PathVariable(name = "id") String storageName,
                                            @RequestParam(name = "force", required = false) Boolean forceDelete)
        throws ModuleException {
        // initialize sessionOwner and session
        // By default, sessionOwner is the user requesting the deletion of the files
        String sessionOwner = authenticationResolver.getUser();
        String session = String.format("Delete %s files %s", storageName, OffsetDateTime.now());
        // order deletion of files

        storageLocationService.deleteFiles(storageName,
                                           forceDelete != null ? forceDelete : false,
                                           sessionOwner,
                                           session);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to retry all files requests in error state for the given storage location and the given request type
     *
     * @param storageName storage location name
     * @param type        {@link FileRequestType} to retry
     */
    @GetMapping(path = ID_PATH + FILES + RETRY)
    @Operation(summary = "Retry file requests in error state",
               description = "Retry all file requests that are in an error state for the specified storage location and request type.")
    @ApiResponse(responseCode = "200", description = "File requests in error state were retried successfully.")
    @ResourceAccess(description = "Retry all files requests in error state for the given storage location and the given request type",
                    role = DefaultRole.ADMIN)
    public ResponseEntity<Void> retryErrors(@PathVariable(name = "id") String storageName,
                                            @PathVariable(name = "type") FileRequestType type) throws ModuleException {
        storageLocationService.retryErrors(storageName, type);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * End-point to retry all files requests in error state for a given source and session
     *
     * @param source  name of the source
     * @param session name of the session
     * @return Void
     */
    @GetMapping(path = RETRY_SESSION)
    @Operation(summary = "Retry file requests in error state for a source and session",
               description = "Retry all file requests that are in an error state for the specified source and session.")
    @ApiResponse(responseCode = "200",
                 description = "File requests in error state for the given source and session were retried successfully.")
    @ResourceAccess(description = "Retry all files requests in error state for a given source and session",
                    role = DefaultRole.ADMIN)
    public ResponseEntity<Void> retryErrorsBySourceAndSession(@PathVariable(name = "source") String source,
                                                              @PathVariable(name = "session") String session) {
        storageLocationService.retryErrorsBySourceAndSession(source, session);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Manually run storage location monitoring (computing number of files, occupation ...)
     *
     * @param reset true if the monitoring must be deleted and recreated
     */
    @GetMapping(path = RUN_MONITORING)
    @Operation(summary = "Manually run storage location monitoring",
               description = "Run monitoring for storage locations to compute the number of files and space occupation. Optionally reset the monitoring.")
    @ApiResponse(responseCode = "200", description = "Storage location monitoring triggered successfully.")
    @ResourceAccess(description = "Manually run storage location monitoring.", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> runMonitoring(@RequestParam(name = RESET_PARAM, required = false) Boolean reset) {
        storageLocationService.monitorStorageLocations(reset != null ? reset : false);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<StorageLocationDto> toResource(StorageLocationDto location, Object... extras) {
        EntityModel<StorageLocationDto> resource = EntityModel.of(location);
        if ((location.getName() != null)) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    METHOD_DELETE_FILES,
                                    LinkRelation.of(METHOD_DELETE_FILES),
                                    MethodParamFactory.build(String.class, location.getName()),
                                    MethodParamFactory.build(Boolean.class));
            return resource;
        }
        StorageType type = location.getConfiguration() != null ?
            location.getConfiguration().getStorageType() :
            StorageType.OFFLINE;
        if (type != StorageType.OFFLINE) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    METHOD_DELETE_FILES,
                                    LinkRelation.of(METHOD_DELETE_FILES),
                                    MethodParamFactory.build(String.class, location.getName()),
                                    MethodParamFactory.build(Boolean.class));
        }
        // If storage location is configured so delete & edit End-point is also available
        if ((location.getConfiguration() != null)) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    METHOD_UPDATE_LOCATION_CONFIGURATION,
                                    LinkRels.UPDATE,
                                    MethodParamFactory.build(String.class, location.getName()),
                                    MethodParamFactory.build(StorageLocationDto.class));
            resourceService.addLink(resource,
                                    this.getClass(),
                                    METHOD_DELETE,
                                    LinkRels.DELETE,
                                    MethodParamFactory.build(String.class, location.getName()));
        } else {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    METHOD_CONFIGURE_LOCATION,
                                    LinkRels.UPDATE,
                                    MethodParamFactory.build(StorageLocationDto.class));
        }
        return resource;
    }
}
