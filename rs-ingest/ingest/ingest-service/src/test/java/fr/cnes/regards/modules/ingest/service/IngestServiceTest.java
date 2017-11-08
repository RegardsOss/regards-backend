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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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
 * @author Sébastien Binda
 */
public class IngestServiceTest extends AbstractSIPTest {

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private ISIPService sipService;

    private final static String SESSION_ID = "sessionId";

    private final static String PROCESSING = "processingChain";

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
    @Requirement("REGARDS_DSL_ING_PRO_710")
    @Purpose("Store SIP checksum and prevent from submitting twice")
    @Test
    public void ingestWithCollision() throws ModuleException {

        SIPCollectionBuilder colBuilder = new SIPCollectionBuilder(PROCESSING, SESSION_ID);
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

        Page<SIPEntity> page = sipService.getSIPEntities(null, SESSION_ID, null, null, null, new PageRequest(0, 10));
        Assert.assertTrue(page.getNumberOfElements() == 1);
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
        Collection<SIPEntity> sips = sipService.getAllVersions(sipId);
        Assert.assertNotNull(sips);
        Assert.assertTrue(sips.size() == sipNb);

        Page<SIPEntity> page = sipService.getSIPEntities(null, null, null, null, null, new PageRequest(0, 10));
        Assert.assertTrue(page.getNumberOfElements() == sipNb);
    }

    @Purpose("Manage ingestion retry after error")
    @Test
    public void retryIngest() throws NoSuchAlgorithmException, IOException, ModuleException {
        // Simulate a SIP in CREATED state
        SIPEntity sip = createSIP("RETY_SIP_001", SESSION_ID, PROCESSING, "admin", 1);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getIpId());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry a running ingest");
        } catch (ModuleException e) {
            // Tothing to do
        }
        sip.setState(SIPState.QUEUED);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getIpId());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry a running ingest");
        } catch (ModuleException e) {
            // Tothing to do
        }

        sip.setState(SIPState.INCOMPLETE);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getIpId());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry a incompletly stored ingest");
        } catch (ModuleException e) {
            // Tothing to do
        }

        sip.setState(SIPState.INDEXED);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getIpId());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry a indexed ingest");
        } catch (ModuleException e) {
            // Tothing to do
        }

        sip.setState(SIPState.AIP_CREATED);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getIpId());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry a running ingest");
        } catch (ModuleException e) {
            // Tothing to do
        }

        sip.setState(SIPState.STORED);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getIpId());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry a stored ingest");
        } catch (ModuleException e) {
            // Tothing to do
        }

        sip.setState(SIPState.VALID);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getIpId());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry a running ingest");
        } catch (ModuleException e) {
            // Tothing to do
        }

        sip.setState(SIPState.STORE_ERROR);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getIpId());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry a store error ingest");
        } catch (ModuleException e) {
            // Tothing to do
        }

        sip.setState(SIPState.REJECTED);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getIpId());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry a rejected ingest");
        } catch (ModuleException e) {
            // Tothing to do
        }

        // Simulate a SIP in AIP_GEN_ERROR error
        sip.setState(SIPState.AIP_GEN_ERROR);
        sip = sipRepository.save(sip);
        ingestService.retryIngest(sip.getIpId());
        sip = sipRepository.findOne(sip.getId());
        Assert.assertTrue("After retry SIP should be reset to state CREATED", SIPState.CREATED.equals(sip.getState()));

        // Simulate a SIP in AIP_GEN_ERROR error
        sip.setState(SIPState.INVALID);
        sip = sipRepository.save(sip);
        ingestService.retryIngest(sip.getIpId());
        sip = sipRepository.findOne(sip.getId());
        Assert.assertTrue("After retry SIP should be reset to state CREATED", SIPState.CREATED.equals(sip.getState()));

    }

    private void ingestNextVersion(String sipId, Integer version)
            throws NoSuchAlgorithmException, IOException, ModuleException {

        String sipFilename = "sip" + version + ".xml";

        SIPCollectionBuilder colBuilder = new SIPCollectionBuilder(PROCESSING, SESSION_ID);
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
