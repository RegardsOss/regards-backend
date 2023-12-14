/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import io.vavr.control.Try;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Sébastien Binda
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_rest_it",
                                   "regards.storage.quota.report.tick=1",
                                   "regards.amqp.enabled=true" })
@ActiveProfiles(value = { "testAmqp", "default", "test" }, inheritProfiles = false)
public class FileReferenceControllerIT extends AbstractFileReferenceControllerIT {

    @Test
    public void getLocationsWithChecksums() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk().expectToHaveSize("$.*", 1);
        Set<String> checksums = new HashSet<>();
        checksums.add(storedFileChecksum);
        performDefaultPost(FileReferenceController.FILE_PATH + FileReferenceController.LOCATIONS_PATH,
                           checksums,
                           requestBuilderCustomizer,
                           "File reference should have been found",
                           TARGET_STORAGE);
    }

    @Test
    public void getLocationsWithUnknownChecksums() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk().expectIsEmpty("$");
        Set<String> checksums = new HashSet<>();
        checksums.add(UUID.randomUUID().toString());
        performDefaultPost(FileReferenceController.FILE_PATH + FileReferenceController.LOCATIONS_PATH,
                           checksums,
                           requestBuilderCustomizer,
                           "File reference should have been found",
                           TARGET_STORAGE);
    }

    @Test
    public void downloadFileError() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusNotFound();
        performDefaultGet(FileReferenceController.FILE_PATH + FileReferenceController.DOWNLOAD_PATH,
                          requestBuilderCustomizer,
                          "File download response status should be NOT_FOUND.",
                          UUID.randomUUID().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_130")
    @Requirement("REGARDS_DSL_STO_ARC_200")
    @Purpose("Check file download")
    public void downloadFileSuccess() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultGet(FileReferenceController.FILE_PATH + FileReferenceController.DOWNLOAD_PATH,
                          requestBuilderCustomizer,
                          "File download response status should be OK",
                          storedFileChecksum);
    }

    @Test
    public void download_failed_cause_quota_max_exceeded() {

        tenantResolver.forceTenant(getDefaultTenant());

        String userEmail = UUID.randomUUID().toString();
        long maxQuota = 5L;
        long rateLimit = 10_000L;
        quotaRepository.save(new DownloadQuotaLimits(getDefaultTenant(), userEmail, maxQuota, rateLimit));

        String urlTemplate = FileReferenceController.FILE_PATH + FileReferenceController.DOWNLOAD_PATH;
        String authToken = manageSecurity(getDefaultTenant(),
                                          urlTemplate,
                                          RequestMethod.GET,
                                          userEmail,
                                          getDefaultRole());

        LongStream.range(0, maxQuota + 1).forEach(i -> {
            if (i < maxQuota) {
                RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
                performGet(urlTemplate,
                           authToken,
                           requestBuilderCustomizer,
                           "File download response status should be OK",
                           storedFileChecksum);
            } else {
                assertEquals("No notification should be present at this point", 0, notificationEvents.get());
                RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatus(HttpStatus.TOO_MANY_REQUESTS);
                performGet(urlTemplate,
                           authToken,
                           requestBuilderCustomizer,
                           "File download response status should be 429",
                           storedFileChecksum);
                // there's been a notification send for that
                Try.run(() -> Thread.sleep(5_000)); // wait for batch reporting, at most 1 sec as per this test properties
                assertEquals("A notification should have been sent on quota exceeded", 1, notificationEvents.get());
            }
        });
    }

    @Test
    public void download_failed_cause_rate_limit_exceeded() {

        tenantResolver.forceTenant(getDefaultTenant());

        String userEmail = UUID.randomUUID().toString();
        quotaRepository.save(new DownloadQuotaLimits(getDefaultTenant(), userEmail, 10L, 0L));

        String urlTemplate = FileReferenceController.FILE_PATH + FileReferenceController.DOWNLOAD_PATH;
        String authToken = manageSecurity(getDefaultTenant(),
                                          urlTemplate,
                                          RequestMethod.GET,
                                          userEmail,
                                          getDefaultRole());

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatus(HttpStatus.TOO_MANY_REQUESTS);
        performGet(urlTemplate,
                   authToken,
                   requestBuilderCustomizer,
                   "File download response status should be 429",
                   storedFileChecksum);
        // there's been a notification send for that
        Try.run(() -> Thread.sleep(5_000)); // wait for batch reporting, at most 1 sec as per this test properties
        assertEquals("A notification should have been sent on rate exceeded", 1, notificationEvents.get());
    }

}
