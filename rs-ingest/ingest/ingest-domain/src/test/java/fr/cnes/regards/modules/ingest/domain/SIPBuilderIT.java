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
package fr.cnes.regards.modules.ingest.domain;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * Test building, serializing and deserializing SIP feature.
 *
 * @author Marc Sordi
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = GsonAutoConfiguration.class)
@TestPropertySource(
    properties = { "regards.cipher.iv=1234567812345678", "regards.cipher.keyLocation=src/test/resources/testKey" })
public class SIPBuilderIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPBuilderIT.class);

    private static final String CATEGORY = "category";

    private static final Set<String> CATEGORIES = Sets.newHashSet(CATEGORY);

    @Autowired
    private Gson gson;

    @Test
    public void createSIPByValue() {

        // Ingestion metadata
        String sessionOwner = "sessionOwner";
        String session = "firstSession";
        String ingestChain = "chain";

        String fileName = "test.xml";
        DataType dataType = DataType.RAWDATA;
        String checksum = "checksum";
        String algorithm = "checksumAlgorithm";

        // Initialize a SIP Collection builder
        SIPCollection collection = SIPCollection.build(IngestMetadataDto.build(sessionOwner,
                                                                               session,
                                                                               ingestChain,
                                                                               CATEGORIES,
                                                                               null,
                                                                               null,
                                                                               StorageMetadata.build("test")));

        // Create a SIP builder
        String providerId = "SIP_001";
        SIP sip = SIP.build(EntityType.DATA, providerId);

        // Fill in required content information
        sip.withDataObject(dataType, Paths.get(fileName), algorithm, checksum);
        sip.registerContentInformation();

        // Add SIP to its collection
        collection.add(sip);

        String collectionString = gson.toJson(collection);
        LOGGER.debug(collectionString);

        // Read SIPs
        SIPCollection sips = gson.fromJson(collectionString, SIPCollection.class);
        Assert.assertTrue(sips.getFeatures().size() == 1);
        Assert.assertTrue(sips.getFeatures().get(0) instanceof SIP);

        SIP one = sips.getFeatures().get(0);
        Assert.assertTrue(providerId.equals(one.getId()));
        Assert.assertNotNull(one.getProperties());

        List<ContentInformation> cisOne = one.getProperties().getContentInformations();
        Assert.assertNotNull(cisOne);
        Assert.assertTrue(cisOne.size() == 1);

        ContentInformation ciOne = cisOne.iterator().next();
        Assert.assertNotNull(ciOne);
        Assert.assertNotNull(ciOne.getDataObject());
        Assert.assertNull(ciOne.getRepresentationInformation());

        OAISDataObject dataObject = ciOne.getDataObject();
        Assert.assertEquals(dataType, dataObject.getRegardsDataType());
        Assert.assertEquals(algorithm, dataObject.getAlgorithm());
        Assert.assertEquals(checksum, dataObject.getChecksum());
    }

    @Test
    public void createSIPByReference() {

        String providerId = "refSip";
        SIP ref = SIP.buildReference(EntityType.DATA, providerId, Paths.get("ref.xml"), "algo", "123456789a");

        String refString = gson.toJson(ref);
        LOGGER.debug(refString);
    }
}
