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
package fr.cnes.regards.modules.access.services.rest.user;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.UUID;

import static fr.cnes.regards.modules.access.services.rest.user.mock.StorageRestClientMock.*;

/**
 * Integration tests for StorageDownloadQuota REST Controller.
 */
@MultitenantTransactional
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=access"},
    locations = { "classpath:application-test.properties" })
public class StorageDownloadQuotaControllerIT extends AbstractRegardsTransactionalIT {

    @Test
    public void getQuotaLimits_for_specific_user() {
        RequestBuilderCustomizer customizer =
            customizer()
                .expectStatusOk()
                .expectValue("$.email", USER_QUOTA_LIMITS_STUB_EMAIL)
                .expectValue("$.maxQuota", USER_QUOTA_LIMITS_STUB_MAX_QUOTA)
                .expectValue("$.rateLimit", USER_QUOTA_LIMITS_STUB_RATE_LIMIT)
            ;
        performDefaultGet(PATH_USER_QUOTA, customizer, "Failed to get user quota limits", USER_QUOTA_LIMITS_STUB_EMAIL);
    }

    @Test
    public void upsertQuotaLimits() {
        DownloadQuotaLimitsDto dto = new DownloadQuotaLimitsDto(
            USER_QUOTA_LIMITS_STUB_EMAIL,
            USER_QUOTA_LIMITS_STUB_MAX_QUOTA,
            USER_QUOTA_LIMITS_STUB_RATE_LIMIT
        );
        RequestBuilderCustomizer customizer =
            customizer()
                .expectStatusOk()
                .expectValue("$.email", dto.getEmail())
                .expectValue("$.maxQuota", dto.getMaxQuota())
                .expectValue("$.rateLimit", dto.getRateLimit())
            ;
        performDefaultPut(PATH_USER_QUOTA, dto, customizer, "Failed to upsert user quota limits", USER_QUOTA_LIMITS_STUB_EMAIL);
    }

    @Test
    public void getQuotaLimits_for_current_user() {
        String userEmail = UUID.randomUUID().toString();

        RequestBuilderCustomizer customizer =
            customizer()
                .expectStatusOk()
                .expectValue("$.email", userEmail)
                .expectValue("$.maxQuota", USER_QUOTA_LIMITS_STUB_MAX_QUOTA)
                .expectValue("$.rateLimit", USER_QUOTA_LIMITS_STUB_RATE_LIMIT)
            ;

        String token = manageSecurity(getDefaultTenant(), PATH_QUOTA, RequestMethod.GET, userEmail, getDefaultRole());
        performGet(PATH_QUOTA, token, customizer, "Failed to get current user quota limits");
    }

    @Test
    public void getCurrentQuotas() {
        String userEmail = UUID.randomUUID().toString();

        RequestBuilderCustomizer customizer =
            customizer()
                .expectStatusOk()
                .expectValue("$.email", userEmail)
                .expectValue("$.maxQuota", USER_QUOTA_LIMITS_STUB_MAX_QUOTA)
                .expectValue("$.rateLimit", USER_QUOTA_LIMITS_STUB_RATE_LIMIT)
                .expectValue("$.currentQuota", CURRENT_USER_QUOTA_STUB)
                .expectValue("$.currentRate", CURRENT_USER_RATE_STUB)
            ;

        String token = manageSecurity(getDefaultTenant(), PATH_CURRENT_QUOTA, RequestMethod.GET, userEmail, getDefaultRole());
        performGet(PATH_CURRENT_QUOTA, token, customizer, "Failed to get current user quotas");
    }
}
