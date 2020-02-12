/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.flow;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;

/**
 *
 * Test storage event handling
 *
 * @author Marc SORDI
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storeresponse",
        "regards.amqp.enabled=true", "regards.scheduler.pool.size=4", "eureka.client.enabled=false" })
@ActiveProfiles({ "testAmqp", "StorageClientMock" })
public class StorageResponseFlowHandlerIT extends IngestMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageResponseFlowHandlerIT.class);

    public static final String MD5_ALGORITHM = "MD5";

    private static final List<String> CATEGORIES = Lists.newArrayList("TEST_CAT");

    private static final String TARGET_STORAGE_ID = "DISK";

    private static final Path DATA_REPOSITORY = Paths.get("target", "data");

    private static final Integer NB_SIPS = 10;

    @Autowired
    private StorageClientMock storageClientMock;

    @Override
    protected void doInit() throws Exception {

        storageClientMock.setBehavior(true, true);

        simulateApplicationReadyEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        if (Files.exists(DATA_REPOSITORY)) {
            // Delete directory recursively
            Files.walk(DATA_REPOSITORY).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        // Create data repository
        Files.createDirectory(DATA_REPOSITORY);
    }

    @Test
    public void test() {

        // Publish SIP
        long start = System.currentTimeMillis();
        for (int i = 0; i < NB_SIPS; i++) {
            SIP sip = create(i, null);
            publishSIPEvent(sip, TARGET_STORAGE_ID, "session", "sessionOwner", CATEGORIES);
        }
        // Wait
        ingestServiceTest.waitForIngestion(NB_SIPS, NB_SIPS * 1000);
        LOGGER.info("{} SIP(s) INGESTED in {} ms", NB_SIPS, System.currentTimeMillis() - start);

        // Wait for storage responses
        waitIngestRequests(0, 10_000L, null);
        LOGGER.info("No request remaining at the moment");

        // Simulate copies of files and AIPs

        // Check update requests

    }

    public void waitIngestRequests(long requests, long timeout, InternalRequestState sipState) {
        // Wait
        long requestCount;
        do {
            requestCount = ingestRequestRepository.count();
            LOGGER.info("{} Ingest request(s) created in database", requestCount);
            if (requestCount == requests) {
                break;
            }
        } while (true);
    }

    protected SIP create(int i, List<String> tags) {
        // Init provider id
        String providerId = String.format("provider_%07d.test", i);

        // Init data file
        Path dataFile = DATA_REPOSITORY.resolve(String.format("file_%07d.test", i));
        try (RandomAccessFile raf = new RandomAccessFile(dataFile.toFile(), "rw")) {
            raf.write(String.format("I'm test file number %07d", i).getBytes());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        // Compute checksum
        String checksum = "TBD";
        try (InputStream input = Files.newInputStream(dataFile)) {
            checksum = ChecksumUtils.computeHexChecksum(input, MD5_ALGORITHM);
        } catch (NoSuchAlgorithmException | IOException e) {
            Assert.fail(e.getMessage());
        }

        SIP sip = SIP.build(EntityType.DATA, providerId);
        sip.withDataObject(DataType.RAWDATA, dataFile, MD5_ALGORITHM, checksum);
        sip.withSyntax(MediaType.APPLICATION_JSON_UTF8);
        sip.registerContentInformation();
        if ((tags != null) && !tags.isEmpty()) {
            sip.withContextTags(tags.toArray(new String[0]));
        }

        // Add creation event
        sip.withEvent(String.format("SIP %s generated", providerId));

        return sip;
    }

    //    private void createRandomFile() throws IOException {
    //
    //        Files.createDirectory(DATA_REPOSITORY);
    //
    //        for (int i = 0; i < NB_FILES; i++) {
    //            Path path = DATA_REPOSITORY.resolve(String.format("file_%07d.test", i));
    //            try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw")) {
    //                raf.write(String.format("I'm test file number %07d", i).getBytes());
    //            } catch (Exception e) {
    //                Assert.fail(e.getMessage());
    //            }
    //        }
    //    }
}
