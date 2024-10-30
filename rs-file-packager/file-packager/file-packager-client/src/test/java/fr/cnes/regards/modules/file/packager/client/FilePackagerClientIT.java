package fr.cnes.regards.modules.file.packager.client;/*
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

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test class for {@link IFilePackagerClient}
 *
 * @author Thibaud Michaudel
 **/
@ActiveProfiles(value = { "default", "test", "nojobs", "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=file_packager_client_it" })
public class FilePackagerClientIT extends AbstractRegardsWebIT {

    private IFilePackagerClient client;

    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private Gson gson;

    @MockBean
    FilePackagerService filePackagerService;

    @Value("${server.address}")
    private String serverAddress;

    @Before
    public void init() {
        client = FeignClientBuilder.build(new TokenClientProvider<>(IFilePackagerClient.class,
                                                                    "http://" + serverAddress + ":" + getPort(),
                                                                    feignSecurityManager),
                                          gson,
                                          requestTemplate -> requestTemplate.header("Content-Type",
                                                                                    MediaType.APPLICATION_JSON_VALUE));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        FeignSecurityManager.asSystem();
    }

    @Test
    public void test_schedule_jobs() {
        // WHEN
        ResponseEntity<Void> response = client.scheduleCompletePackage();

        // THEN
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Mockito.verify(filePackagerService, Mockito.times(1)).scheduleStoreCompletePackageJobs();

    }

}
