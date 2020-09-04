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
package fr.cnes.regards.modules.storage.rest;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=quota_rest_it",
        "regards.storage.cache.path=target/cache" })
@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
public class DownloadQuotaControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadQuotaControllerIT.class);

    @Autowired
    private IDownloadQuotaRepository quotaRepository;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());
    }

    @After
    public void clear() {
        quotaRepository.deleteAll();
    }

    @Test
    public void createQuotaLimitsError() {
        RequestBuilderCustomizer customizer =
            customizer()
                .expectStatusCreated();

        performDefaultPost(
            DownloadQuotaController.PATH_QUOTA,
            new DownloadQuotaLimitsDto(
                "foo@bar.com",
                50L,
                10L
            ),
            customizer,
            "Create quota limits error."
        );
    }
}
