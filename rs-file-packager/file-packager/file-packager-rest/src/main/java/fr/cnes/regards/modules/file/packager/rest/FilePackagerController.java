/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.file.packager.rest;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.file.packager.service.scheduler.CompletePackageScheduler;
import fr.cnes.regards.modules.file.packager.service.scheduler.RetryFilePackagingScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller for file packager actions
 *
 * @author Thibaud Michaudel
 **/
@RestController
@RequestMapping(FilePackagerController.BASE_PATH)
public class FilePackagerController {

    public static final String BASE_PATH = "/file-packager";

    public static final String SCHEDULE_COMPLETE_PACKAGE_PATH = "/storeCompletePackages";

    public static final String RETRY_PACKAGE_ERROR_PATH = "/retryPackageError";

    private final CompletePackageScheduler completePackageScheduler;

    private final RetryFilePackagingScheduler retryFilePackagingScheduler;

    public FilePackagerController(CompletePackageScheduler completePackageScheduler,
                                  RetryFilePackagingScheduler retryFilePackagingScheduler) {
        this.completePackageScheduler = completePackageScheduler;
        this.retryFilePackagingScheduler = retryFilePackagingScheduler;
    }

    @Operation(summary = "Request to schedule storage of small file packages that are complete (or too old)")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Successfully scheduled complete package storage jobs") })
    @PostMapping(path = SCHEDULE_COMPLETE_PACKAGE_PATH)
    @ResourceAccess(description = "Endpoint to schedule storage request of small file packages that are complete (or too old)",
                    role = DefaultRole.ADMIN)
    public ResponseEntity<Void> scheduleStoreCompletePackage() {
        completePackageScheduler.scheduleCompletePackage();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Request to reschedule the storage of small file packages whose last storage attempt ended in error")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Successfully rescheduled the storage small file packages in error") })
    @PostMapping(path = RETRY_PACKAGE_ERROR_PATH)
    @ResourceAccess(description = "Endpoint to reschedule the storage of small file packages whose last storage attempt ended in error",
                    role = DefaultRole.ADMIN)
    public ResponseEntity<Void> retryStorePackagesInError() {
        retryFilePackagingScheduler.scheduleRetryFilePackaging();
        return ResponseEntity.ok().build();
    }
}
