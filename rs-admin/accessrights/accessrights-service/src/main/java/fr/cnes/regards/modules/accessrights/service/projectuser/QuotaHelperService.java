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
package fr.cnes.regards.modules.accessrights.service.projectuser;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.modules.fileaccess.dto.quota.DownloadQuotaLimitsDto;
import fr.cnes.regards.modules.storage.client.StorageDownloaderClient;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Component
public class QuotaHelperService {

    private static final Logger LOG = LoggerFactory.getLogger(QuotaHelperService.class);

    private static final Long MAX_QUOTA_DEFAULT_VALUE = -1L;

    private final StorageDownloaderClient storageDownloaderClient;

    public QuotaHelperService(StorageDownloaderClient storageDownloaderClient) {
        this.storageDownloaderClient = storageDownloaderClient;
    }

    /**
     * Returns the default quota, or null if it can not be determined (an error is logged in this case).
     */
    public @Nullable Long getDefaultQuota() {

        Long defaultQuota = MAX_QUOTA_DEFAULT_VALUE;

        try {
            FeignSecurityManager.asSystem();

            ResponseEntity<Long> response = storageDownloaderClient.getMaxQuota();
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                defaultQuota = response.getBody();
            }
        } catch (Exception e) {
            LOG.warn("Unable to retrieve default quota value from storage service - using default value", e);
        } finally {
            FeignSecurityManager.reset();
        }

        return defaultQuota;
    }

    /**
     * Returns the downloads quota for the specified user. Returns null if a problem prevents
     * from determining this quota (an error is logged in this case).
     */
    public @Nullable DownloadQuotaLimitsDto getQuota(String userEmail) {
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<DownloadQuotaLimitsDto> storageResponse = storageDownloaderClient.getQuotaLimits(userEmail);
            if (storageResponse.getStatusCode().is2xxSuccessful()) {
                return ResponseEntityUtils.extractBodyOrThrow(storageResponse,
                                                              "Cannot get quota limit of " + userEmail);
            } else {
                LOG.warn("Unable to retrieve quota value for user {} from storage service", userEmail);
                return null;
            }
        } catch (HttpServerErrorException | HttpClientErrorException | ModuleException e) {
            LOG.warn("Unable to retrieve quota value for user {} from storage service", userEmail, e);
            return null;
        } finally {
            FeignSecurityManager.reset();
        }
    }

}
