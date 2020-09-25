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

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static fr.cnes.regards.modules.storage.client.IStorageDownloadQuotaClient.*;

@RestController
public class StorageDownloadQuotaController {

    /**
     * Client handling storage quotas
     */
    @Autowired
    private IStorageRestClient storageClient;

    @GetMapping(path = PATH_USER_QUOTA)
    @ResponseBody
    @ResourceAccess(description = "Get user download quota limits.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(
        @PathVariable(USER_EMAIL_PARAM) String userEmail
    ) {
        return storageClient.getQuotaLimits(userEmail);
    }

    @PutMapping(path = PATH_USER_QUOTA)
    @ResponseBody
    @ResourceAccess(description = "Update user download quota limits.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<DownloadQuotaLimitsDto> upsertQuotaLimits(
        @PathVariable(USER_EMAIL_PARAM) String userEmail,
        @Valid @RequestBody DownloadQuotaLimitsDto quotaLimits
    ) {
        return storageClient.upsertQuotaLimits(userEmail, quotaLimits);
    }

    @GetMapping(path = PATH_QUOTA)
    @ResponseBody
    @ResourceAccess(description = "Get current user download quota limits.", role = DefaultRole.PUBLIC)
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits() {
        return storageClient.getQuotaLimits();
    }

    @GetMapping(path = PATH_CURRENT_QUOTA)
    @ResponseBody
    @ResourceAccess(description = "Get current download quota values for current user.", role = DefaultRole.PUBLIC)
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas() {
        return storageClient.getCurrentQuotas();
    }
}
