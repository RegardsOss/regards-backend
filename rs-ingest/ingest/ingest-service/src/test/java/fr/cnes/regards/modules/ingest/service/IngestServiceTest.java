/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * @author Marc Sordi
 */
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:test.properties")
@ContextConfiguration(classes = { IngestServiceTest.IngestConfiguration.class })
public class IngestServiceTest extends AbstractDaoTransactionalTest {

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IAIPRepository aipRepository;

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules" })
    static class IngestConfiguration {
    }

    @Autowired
    private IIngestService ingestService;

    @Before
    public void init() {
        aipRepository.deleteAll();
        sipRepository.deleteAll();
    }

    /**
     * Check if service properly store SIP and prevent to store a SIP twice
     * @throws ModuleException if error occurs!
     */
    @Requirement("REGARDS_DSL_ING_PRO_240")
    @Requirement("REGARDS_DSL_ING_PRO_250")
    @Purpose("Store SIP checksum and prevent from submitting twice")
    @Test
    public void ingestWithCollision() throws ModuleException {

        SIPCollectionBuilder colBuilder = new SIPCollectionBuilder("processingChain", "sessionId");
        SIPCollection collection = colBuilder.build();

        SIPBuilder builder = new SIPBuilder("SIP_001");
        collection.add(builder.buildReference(Paths.get("sip1.xml"), "zaasfsdfsdlfkmsldgfml12df"));

        // First ingestion
        Collection<SIPEntity> results = ingestService.ingest(collection);
        Assert.assertNotNull(results);
        Assert.assertTrue(results.size() == 1);
        SIPEntity one = results.iterator().next();
        Assert.assertTrue(one.getVersion() == 1);
        Assert.assertTrue(SIPState.CREATED.equals(one.getState()));

        // Re-ingest same SIP
        results = ingestService.ingest(collection);
        Assert.assertNotNull(results);
        Assert.assertTrue(results.size() == 1);
        SIPEntity two = results.iterator().next();
        Assert.assertTrue(two.getVersion() == 2);
        Assert.assertTrue(SIPState.REJECTED.equals(two.getState()));
    }

    /**
     * Check if service properly manage SIP versions
     * @throws ModuleException if error occurs!
     * @throws IOException if error occurs!
     * @throws NoSuchAlgorithmException if error occurs!
     */
    @Requirement("REGARDS_DSL_ING_PRO_200")
    @Requirement("REGARDS_DSL_ING_PRO_220")
    @Purpose("Manage SIP versionning")
    @Test
    public void ingestWithUpdate() throws ModuleException, NoSuchAlgorithmException, IOException {

        String sipId = "sipToUpdate";
        int sipNb = 3;

        for (int i = 1; i <= sipNb; i++) {
            ingestNextVersion(sipId, i);
        }

        // Check sipNb SIP are stored
        Collection<SIPEntity> sips = ingestService.getAllVersions(sipId);
        Assert.assertNotNull(sips);
        Assert.assertTrue(sips.size() == sipNb);
    }

    private void ingestNextVersion(String sipId, Integer version)
            throws NoSuchAlgorithmException, IOException, ModuleException {

        String sipFilename = "sip" + version + ".xml";

        SIPCollectionBuilder colBuilder = new SIPCollectionBuilder("processingChain", "sessionId");
        SIPCollection collection = colBuilder.build();

        SIPBuilder builder = new SIPBuilder(sipId);
        collection.add(builder.buildReference(Paths.get(sipFilename), ChecksumUtils
                .computeHexChecksum(new ByteArrayInputStream(sipFilename.getBytes()), IngestService.MD5_ALGORITHM)));

        Collection<SIPEntity> results = ingestService.ingest(collection);
        Assert.assertNotNull(results);
        Assert.assertTrue(results.size() == 1);
        SIPEntity one = results.iterator().next();
        Assert.assertTrue(one.getVersion() == version);
        Assert.assertTrue(SIPState.CREATED.equals(one.getState()));
    }
}
