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
package fr.cnes.regards.modules.ingest.domain;

import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;

import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;

/**
 * Test building, serializing and deserializing SIP feature.
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration(exclude = { JpaRepositoriesAutoConfiguration.class })
@TestPropertySource(properties = { "regards.cipher.iv=1234567812345678", "regards.cipher.keyLocation=src/test/resources/testKey"})
public class SIPBuilderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPBuilderTest.class);

    @Autowired
    private Gson gson;

    @Test
    public void createSIPByValue() {

        // Ingestion metadata
        String processingChain = "chain";
        String sessionId = "firstSession";

        String fileName = "test.xml";
        DataType dataType = DataType.RAWDATA;
        String checksum = "checksum";
        String algorithm = "checksumAlgorithm";

        // Initialize a SIP Collection builder
        SIPCollectionBuilder collectionBuilder = new SIPCollectionBuilder(processingChain, sessionId);

        // Create a SIP builder
        String providerId = "SIP_001";
        SIPBuilder sipBuilder = new SIPBuilder(providerId);

        // Fill in required content information
        sipBuilder.getContentInformationBuilder().setDataObject(dataType, Paths.get(fileName), algorithm, checksum);
        sipBuilder.addContentInformation();

        // Add SIP to its collection
        collectionBuilder.add(sipBuilder.build());

        SIPCollection collection = collectionBuilder.build();
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
        Assert.assertTrue(dataObject.getUrls().stream().map(url -> url.getPath())
                .filter(path -> path.equals(Paths.get(fileName).toAbsolutePath().toString())).findFirst().isPresent());
        Assert.assertEquals(algorithm, dataObject.getAlgorithm());
        Assert.assertEquals(checksum, dataObject.getChecksum());
    }

    @Test
    public void createSIPByReference() {

        String providerId = "refSip";
        SIPBuilder builder = new SIPBuilder(providerId);
        SIP ref = builder.buildReference(Paths.get("ref.xml"), "algo", "123456789a");

        String refString = gson.toJson(ref);
        LOGGER.debug(refString);
    }
}
