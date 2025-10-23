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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.storage.client;

import feign.Response;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.fileaccess.dto.quota.DownloadQuotaLimitsDto;
import fr.cnes.regards.modules.fileaccess.dto.quota.UserCurrentQuotasDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Wrapper to call the right service for download files.
 * Storage or Downloader service. Use of one or other is defined in configuration by spring property.
 * Downloader service is used to prevent performance issues in storage service during
 * downloading.
 *
 * @author Sébastien Binda
 **/
@Component
public class StorageDownloaderClient {

    public static final String NO_CLIENT_FOR_QUOTA_ERROR = "No storage client available for quota !";

    /**
     * Variable to choose storage or downloader service.
     */

    private final boolean storageDownloaderEnabled;

    /**
     * TODO: Remove use of download and quota management on storage microservice. Use rs-downloader service.
     * To keep until all project users are migrated to downloader service.
     */
    private final IStorageRestClient storageRestClient;

    private final IStorageDownloaderRestClient storageDownloaderRestClient;

    public StorageDownloaderClient(@Autowired(required = false) IStorageRestClient storageRestClient,
                                   @Autowired(required = false)
                                   IStorageDownloaderRestClient storageDownloaderRestClient,
                                   @Value("${regards.storage.downloader.enabled:false}")
                                   boolean storageDownloaderEnabled) {
        this.storageDownloaderEnabled = storageDownloaderEnabled;
        this.storageRestClient = storageRestClient;
        this.storageDownloaderRestClient = storageDownloaderRestClient;

    }

    public Response downloadFile(String checksum, boolean isContentInline) {
        if (storageDownloaderEnabled && storageDownloaderRestClient != null) {
            return storageDownloaderRestClient.downloadFile(checksum, isContentInline);
        }
        if (storageRestClient != null) {
            return storageRestClient.downloadFile(checksum, isContentInline);
        }
        throw new RsRuntimeException("No storage client available for download !");
    }

    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(String userEmail) {
        if (storageDownloaderEnabled && storageDownloaderRestClient != null) {
            return storageDownloaderRestClient.getQuotaLimits(userEmail);
        }
        if (storageRestClient != null) {
            return storageRestClient.getQuotaLimits(userEmail);
        }
        throw new RsRuntimeException(NO_CLIENT_FOR_QUOTA_ERROR);
    }

    public ResponseEntity<DownloadQuotaLimitsDto> upsertQuotaLimits(String userEmail,
                                                                    DownloadQuotaLimitsDto quotaLimits) {
        if (storageDownloaderEnabled && storageDownloaderRestClient != null) {
            return storageDownloaderRestClient.upsertQuotaLimits(userEmail, quotaLimits);
        }
        if (storageRestClient != null) {
            return storageRestClient.upsertQuotaLimits(userEmail, quotaLimits);
        }
        throw new RsRuntimeException(NO_CLIENT_FOR_QUOTA_ERROR);
    }

    public ResponseEntity<List<DownloadQuotaLimitsDto>> getQuotaLimits(String[] userEmails) {
        if (storageDownloaderEnabled && storageDownloaderRestClient != null) {
            return storageDownloaderRestClient.getQuotaLimits(userEmails);
        }
        if (storageRestClient != null) {
            return storageRestClient.getQuotaLimits(userEmails);
        }
        throw new RsRuntimeException(NO_CLIENT_FOR_QUOTA_ERROR);
    }

    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits() {
        if (storageDownloaderEnabled && storageDownloaderRestClient != null) {
            return storageDownloaderRestClient.getQuotaLimits();
        }
        if (storageRestClient != null) {
            return storageRestClient.getQuotaLimits();
        }
        throw new RsRuntimeException(NO_CLIENT_FOR_QUOTA_ERROR);
    }

    public ResponseEntity<UserCurrentQuotasDto> getCurrentQuotas() {
        if (storageDownloaderEnabled && storageDownloaderRestClient != null) {
            return storageDownloaderRestClient.getCurrentQuotas();
        }
        if (storageRestClient != null) {
            return storageRestClient.getCurrentQuotas();
        }
        throw new RsRuntimeException(NO_CLIENT_FOR_QUOTA_ERROR);
    }

    public ResponseEntity<UserCurrentQuotasDto> getCurrentQuotas(String userEmail) {
        if (storageDownloaderEnabled && storageDownloaderRestClient != null) {
            return storageDownloaderRestClient.getCurrentQuotas(userEmail);
        }
        if (storageRestClient != null) {
            return storageRestClient.getCurrentQuotas(userEmail);
        }
        throw new RsRuntimeException(NO_CLIENT_FOR_QUOTA_ERROR);
    }

    public ResponseEntity<Long> getMaxQuota() {
        if (storageDownloaderEnabled && storageDownloaderRestClient != null) {
            return storageDownloaderRestClient.getMaxQuota();
        }
        if (storageRestClient != null) {
            return storageRestClient.getMaxQuota();
        }
        throw new RsRuntimeException(NO_CLIENT_FOR_QUOTA_ERROR);
    }

    public ResponseEntity<List<UserCurrentQuotasDto>> getCurrentQuotasList(String[] userEmails) {
        if (storageDownloaderEnabled && storageDownloaderRestClient != null) {
            return storageDownloaderRestClient.getCurrentQuotasList(userEmails);
        }
        if (storageRestClient != null) {
            return storageRestClient.getCurrentQuotasList(userEmails);
        }
        throw new RsRuntimeException(NO_CLIENT_FOR_QUOTA_ERROR);
    }

}
