/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.storage.client.IStorageDownloadQuotaRestClient;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class QuotaHelperService {

    private static final Logger LOG = LoggerFactory.getLogger(QuotaHelperService.class);

    private static final Long MAX_QUOTA_DEFAULT_VALUE = -1L;

    private final IStorageSettingClient storageSettingClient;

    @Autowired
    IStorageDownloadQuotaRestClient quotaRestClient;

    public QuotaHelperService(IStorageSettingClient storageSettingClient) {
        this.storageSettingClient = storageSettingClient;
    }

    public Long getDefaultQuota() {

        Long defaultQuota = MAX_QUOTA_DEFAULT_VALUE;

        try {
            FeignSecurityManager.asSystem();

            ResponseEntity<Long> response = quotaRestClient.getMaxQuota();
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

}
