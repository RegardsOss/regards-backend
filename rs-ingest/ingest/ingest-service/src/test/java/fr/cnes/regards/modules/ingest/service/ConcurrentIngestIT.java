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
package fr.cnes.regards.modules.ingest.service;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.domain.AbstractOAISEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.service.aip.scheduler.IngestRequestSchedulerService;
import fr.cnes.regards.modules.ingest.service.request.IngestRequestService;
import fr.cnes.regards.modules.ingest.service.request.RequestService;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author Thibaud Michaudel
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest",
                                   "eureka.client.enabled=false",
                                   "regards.ingest.aip.delete.bulk.delay=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles("noscheduler")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class ConcurrentIngestIT extends IngestMultitenantServiceIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentIngestIT.class);

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private Gson gson;

    @Autowired
    IngestRequestSchedulerService ingestRequestSchedulerService;

    @Autowired
    private RequestService requestService;

    @SpyBean
    private IngestRequestService ingestRequestService;

    @Override
    public void doInit() throws ModuleException {
        Mockito.clearInvocations(ingestRequestService);
    }

    @Purpose("Manage SIP versioning when 2 collections are processed at the same time when the second versions "
             + "are ingested first but the submission date is present")
    @Test
    public void ingest_concurrent_versions_with_submissionDate() throws EntityInvalidException {
        // Ingest a sip collection B then a sip collection A
        // B sips are new versions of A sips
        // In the first scheduling, only the 2 A sips should be processed
        // Unblock the blocked requests, then do another scheduling, both the 2 A sips and the 2 B sips are no processed
        testConcurrency("sips/sips-with-date-B.json", "sips/sips-with-date-A.json");
    }

    @Purpose(
        "Manage SIP versioning when 2 collections are processed at the same time when the first versions are ingested first")
    @Test
    public void ingest_concurrent_versions_without_submission_date() throws EntityInvalidException {
        // Ingest a sip collection A then a sip collection B
        // B sips are ew versions of A sips
        // In the first scheduling, only the 2 A sips should be processed
        // Unblock the blocked requests, then do another scheduling, both the 2 A sips and the 2 B sips are no processed
        testConcurrency("sips/sips-without-date-A.json", "sips/sips-without-date-B.json");
    }

    private void testConcurrency(String firstSipCollectionFile, String secondSipCollectionFile)
        throws EntityInvalidException {
        //Given
        JsonReader reader = new JsonReader(new InputStreamReader(Objects.requireNonNull(this.getClass()
                                                                                            .getClassLoader()
                                                                                            .getResourceAsStream(
                                                                                                firstSipCollectionFile))));
        SIPCollection sipCollection = gson.fromJson(reader, SIPCollection.class);

        ingestService.handleSIPCollection(sipCollection);

        reader = new JsonReader(new InputStreamReader(Objects.requireNonNull(this.getClass()
                                                                                 .getClassLoader()
                                                                                 .getResourceAsStream(
                                                                                     secondSipCollectionFile))));
        sipCollection = gson.fromJson(reader, SIPCollection.class);

        ingestService.handleSIPCollection(sipCollection);

        // When
        ingestRequestSchedulerService.scheduleRequests();

        // Then
        ingestServiceTest.waitForIngestion(2, 10000, getDefaultTenant());

        // When
        requestService.unblockRequests(RequestTypeEnum.INGEST);
        ingestRequestSchedulerService.scheduleRequests();

        // Then
        ingestServiceTest.waitForIngestion(4, 10000, getDefaultTenant());
        List<SIPEntity> sips = sipRepository.findAll();
        sips.sort(Comparator.comparing(AbstractOAISEntity::getCreationDate));
        Assertions.assertEquals(4, sips.size(), "Expected 4 sips");
        List<String> actual = sips.stream()
                                  .map(sip -> (String) sip.getSip()
                                                          .getProperties()
                                                          .getDescriptiveInformation()
                                                          .get("name"))
                                  .toList();
        List<String> expected = List.of("Alice First Version",
                                        "Bob First Version",
                                        "Alice Second Version",
                                        "Bob Second Version");
        Assertions.assertTrue(actual.containsAll(expected) && expected.containsAll(actual),
                              "The saved sips are not the expected ones");

        SIPEntity sipA1 = sips.stream()
                              .filter(sip -> sip.getSip()
                                                .getProperties()
                                                .getDescriptiveInformation()
                                                .get("name")
                                                .equals("Alice First Version"))
                              .findFirst()
                              .get();

        SIPEntity sipA2 = sips.stream()
                              .filter(sip -> sip.getSip()
                                                .getProperties()
                                                .getDescriptiveInformation()
                                                .get("name")
                                                .equals("Alice Second Version"))
                              .findFirst()
                              .get();

        SIPEntity sipB1 = sips.stream()
                              .filter(sip -> sip.getSip()
                                                .getProperties()
                                                .getDescriptiveInformation()
                                                .get("name")
                                                .equals("Bob First Version"))
                              .findFirst()
                              .get();

        SIPEntity sipB2 = sips.stream()
                              .filter(sip -> sip.getSip()
                                                .getProperties()
                                                .getDescriptiveInformation()
                                                .get("name")
                                                .equals("Bob Second Version"))
                              .findFirst()
                              .get();

        Assertions.assertEquals(sipA1.getVersion(), 1, "The sip version isn't the expected one");
        Assertions.assertEquals(sipA2.getVersion(), 2, "The sip version isn't the expected one");
        Assertions.assertEquals(sipB1.getVersion(), 1, "The sip version isn't the expected one");
        Assertions.assertEquals(sipB2.getVersion(), 2, "The sip version isn't the expected one");
        Assertions.assertTrue(sipA1.getCreationDate().isBefore(sipA2.getCreationDate()),
                              "The version 1 of the sip " + "should be older than the " + "version 2");
        Assertions.assertTrue(sipB1.getCreationDate().isBefore(sipB2.getCreationDate()),
                              "The version 1 of the sip " + "should be older than the " + "version 2");
    }
}
