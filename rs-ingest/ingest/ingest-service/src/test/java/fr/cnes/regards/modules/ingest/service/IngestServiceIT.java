/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.ingest.domain.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingService;

/**
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class IngestServiceIT extends AbstractSipIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestServiceIT.class);

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private IIngestProcessingService ingestProcessingService;

    @Autowired
    private ISIPService sipService;

    private final static String CLIENT_ID = "sessionSource";

    private final static String CLIENT_SESSION = "sessionName";

    private final static String INGEST_CHAIN = "processingChain";

    @Override
    public void doInit() throws ModuleException {
        ingestProcessingService.initDefaultServiceConfiguration();
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

        LOGGER.debug("Starting test ingestWithCollision");

        SIPCollection collection = SIPCollection.build(IngestMetadata
                .build(CLIENT_ID, CLIENT_SESSION, IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL));
        SIPBuilder builder = new SIPBuilder("SIP_001");
        collection.add(builder.buildReference(Paths.get("sip1.xml"), "zaasfsdfsdlfkmsldgfml12df"));

        // First ingestion
        Collection<SIPDto> results = ingestService.ingest(collection);
        Assert.assertNotNull(results);
        Assert.assertTrue(results.size() == 1);
        SIPDto one = results.iterator().next();
        Assert.assertTrue(one.getVersion() == 1);
        Assert.assertTrue(SIPState.CREATED.equals(one.getState()));

        // Re-ingest same SIP
        results = ingestService.ingest(collection);
        Assert.assertNotNull(results);
        Assert.assertTrue(results.size() == 1);
        SIPDto two = results.iterator().next();
        Assert.assertTrue(two.getVersion() == 2);
        Assert.assertTrue(SIPState.REJECTED.equals(two.getState()));

        Page<SIPEntity> page = sipService.search(null, CLIENT_ID, null, null, null, null, null, PageRequest.of(0, 10));
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

        String providerId = "sipToUpdate";
        int sipNb = 3;

        for (int i = 1; i <= sipNb; i++) {
            ingestNextVersion(providerId, i);
        }

        // Check sipNb SIP are stored
        Collection<SIPEntity> sips = sipService.getAllVersions(providerId);
        Assert.assertNotNull(sips);
        Assert.assertTrue(sips.size() == sipNb);

        Page<SIPEntity> page = sipService.search(null, null, null, null, null, null, null, PageRequest.of(0, 10));
        Assert.assertTrue(page.getNumberOfElements() == sipNb);
    }

    @Purpose("Manage ingestion retry after error")
    @Test
    public void retryIngest() throws NoSuchAlgorithmException, IOException, ModuleException {
        // Simulate a SIP in CREATED state
        SIPEntity sip = createSIP("RETY_SIP_001", CLIENT_ID, CLIENT_SESSION, INGEST_CHAIN, "admin", 1);

        sip.setState(SIPState.CREATED);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getSipIdUrn());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry a created SIP");
        } catch (ModuleException e) {
            // Tothing to do
        }

        sip.setState(SIPState.DELETED);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getSipIdUrn());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry a deleted SIP");
        } catch (ModuleException e) {
            // Tothing to do
        }

        sip.setState(SIPState.QUEUED);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getSipIdUrn());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry a queued SIP");
        } catch (ModuleException e) {
            // Tothing to do
        }

        sip.setState(SIPState.INGESTED);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getSipIdUrn());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry an ingested SIP");
        } catch (ModuleException e) {
            // Tothing to do
        }

        sip.setState(SIPState.REJECTED);
        sip = sipRepository.save(sip);
        try {
            ingestService.retryIngest(sip.getSipIdUrn());
            Assert.fail("There should an EntityLOperationForbidden exception. It is not possible to retry a rejected SIP");
        } catch (ModuleException e) {
            // Tothing to do
        }

        // Simulate a SIP in AIP_GEN_ERROR error
        sip.setState(SIPState.ERROR);
        sip = sipRepository.save(sip);
        ingestService.retryIngest(sip.getSipIdUrn());
        sip = sipRepository.findById(sip.getId()).get();
        Assert.assertEquals(SIPState.CREATED, sip.getState());
    }

    private void ingestNextVersion(String providerId, Integer version)
            throws NoSuchAlgorithmException, IOException, ModuleException {

        String sipFilename = "sip" + version + ".xml";

        SIPCollection collection = SIPCollection.build(IngestMetadata
                .build(CLIENT_ID, CLIENT_SESSION, IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL));

        SIPBuilder builder = new SIPBuilder(providerId);
        collection.add(builder.buildReference(Paths.get(sipFilename), ChecksumUtils
                .computeHexChecksum(new ByteArrayInputStream(sipFilename.getBytes()), IngestService.MD5_ALGORITHM)));

        Collection<SIPDto> results = ingestService.ingest(collection);
        Assert.assertNotNull(results);
        Assert.assertTrue(results.size() == 1);
        SIPDto one = results.iterator().next();
        Assert.assertTrue(one.getVersion() == version);
        Assert.assertTrue(SIPState.CREATED.equals(one.getState()));
    }
}
