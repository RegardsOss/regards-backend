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
package fr.cnes.regards.modules.ingest.service.aip;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.service.IIngestService;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.storage.client.StorageClient;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Test storage of sip files on multiple storage locations
 *
 * @author Iliana Ghazali
 **/
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS,
                hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=aip_storage_service_with_meta_it" },
                    locations = { "classpath:application-test.properties" })
public class AIPStorageServiceWithMetaIT extends IngestMultitenantServiceIT {

    private static final String LOCAL_ALL = "LOCAL-ALL";

    private static final String LOCAL_RAW = "LOCAL-RAW";

    private static final String LOCAL_IMG_MIN = "LOCAL-IMG-MIN";

    private static final String LOCAL_IMG_MAX = "LOCAL-IMG-MAX";

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private Gson gson;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @MockBean
    private StorageClient storageClient;

    @Captor
    ArgumentCaptor<List<FileStorageRequestDTO>> fileStorageRequestsCaptor;

    @Test
    @Purpose("Test if storage requests are correctly stored according to sip metadata.")
    // According to the sip storage metadata (see: src/test/resources/data/sip-with-storage-meta.json), 
    // the related files should be stored on specific storage locations depending on their size and types.
    //
    // storage        | target type                                      | size range (o)
    // _______________|__________________________________________________|_______________
    // LOCAL-ALL      | all                                              | none
    // Local-RAW      | ["RAWDATA", "DOCUMENT", "OTHER"]                 | 10 <= T <= 20
    // Local-IMG-MIN  | ["THUMBNAIL", "QUICKLOOK_MD", "QUICKLOOK_HD"]    | T >= 10
    // Local-IMG-MAX  | ["THUMBNAIL", "QUICKLOOK_MD", "QUICKLOOK_HD"]    | T <= 20

    // files                | file size (o) | type          | ** EXPECTED LOCATION **
    // _____________________|_______________|_________________________________________
    // simple_sip_01.dat    |      10       | RAWDATA       | Local-RAW, LOCAL-ALL
    // simple_document.pdf  |      21       | DOCUMENT      | LOCAL-ALL
    // other.mp4            |      9        | OTHER         | LOCAL-ALL
    // thumbnail-01.jpg     |      21       | THUMBNAIL     | Local-IMG-MIN, LOCAL-ALL
    // quicklook_md.jpg     |      9        | QUICKLOOK_MD  | Local-IMG-MAX, LOCAL-ALL
    // quicklook_hd.jpg     |      no size  | QUICKLOOK_HD  | LOCAL-ALL
    // simple_sip_02.dat    |      20       | RAWDATA       | LOCAL-RAW, LOCAL-ALL
    public void store_on_different_storages_success() throws FileNotFoundException, EntityInvalidException {
        // GIVEN 
        // build sips with storage metadata
        JsonReader input = new JsonReader(new FileReader(Paths.get("src/test/resources/data/sip-with-storage-meta.json")
                                                              .toFile()));
        SIPCollection sips = gson.fromJson(input, SIPCollection.class);
        // WHEN
        // sips are ingested
        ingestService.handleSIPCollection(sips);
        // THEN
        // two aips should be built from sips with storage requests
        Awaitility.await().atMost(10000, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return this.aipRepository.count() == 2;
        });
        Mockito.verify(storageClient, Mockito.times(2)).store(fileStorageRequestsCaptor.capture());
        List<List<FileStorageRequestDTO>> storageReq = fileStorageRequestsCaptor.getAllValues();
        // validate storage requests from aip1
        for (List<FileStorageRequestDTO> storageReqPerAip : storageReq) {
            if (storageReqPerAip.size() == 9) {
                validateStorageRequests(storageReqPerAip,
                                        Map.of("simple_sip_01.dat",
                                               Set.of(LOCAL_ALL, LOCAL_RAW),
                                               "simple_document.pdf",
                                               Set.of(LOCAL_ALL),
                                               "other.mp4",
                                               Set.of(LOCAL_ALL),
                                               "thumbnail-01.jpg",
                                               Set.of(LOCAL_ALL, LOCAL_IMG_MIN),
                                               "quicklook_md.jpg",
                                               Set.of(LOCAL_ALL, LOCAL_IMG_MAX),
                                               "quicklook_hd.jpg",
                                               Set.of(LOCAL_ALL)));
            } else if (storageReqPerAip.size() == 2) {
                // validate storage requests from aip2
                validateStorageRequests(storageReqPerAip, Map.of("simple_sip_02.dat", Set.of(LOCAL_RAW, LOCAL_ALL)));
            } else {
                Assert.fail("Unexpected number of FileStorageRequests.");
            }
        }
    }

    private void validateStorageRequests(List<FileStorageRequestDTO> actualStorageRequests,
                                         Map<String, Set<String>> expectedMappingStorageRequests) {
        Assertions.assertThat(actualStorageRequests.stream()
                                                   .collect(Collectors.groupingBy(FileStorageRequestDTO::getFileName,
                                                                                  Collectors.mapping(
                                                                                      FileStorageRequestDTO::getStorage,
                                                                                      Collectors.toUnmodifiableSet()))))
                  .containsExactlyInAnyOrderEntriesOf(expectedMappingStorageRequests);

    }
}