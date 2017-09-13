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
package fr.cnes.regards.modules.ingest.domain;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;

import fr.cnes.regards.framework.oais.urn.DataType;

/**
 * Test building, serializing and deserializing SIP feature.
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class SIPBuilderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPBuilderTest.class);

    @Autowired
    private Gson gson;

    @Test
    public void createSIPByValue() {

        String url = "url";
        String mimeType = "mimeType";
        DataType dataType = DataType.RAWDATA;
        String checksum = "checksum";
        String checksumAlgorithm = "checksumAlgorithm";

        // Build SIPs
        SIPCollectionBuilder collectionBuilder = new SIPCollectionBuilder();
        collectionBuilder.setProcessing("myChain1");
        collectionBuilder.setSessionId("23");
        SIPCollection collection = collectionBuilder.build();

        SIPBuilder builder = new SIPBuilder();
        builder.addDataObject(url, mimeType, dataType, checksum, checksumAlgorithm);
        collection.add(builder.build());

        String collectionString = gson.toJson(collection);
        LOGGER.debug(collectionString);

        // Read SIPs
        SIPCollection sips = gson.fromJson(collectionString, SIPCollection.class);
        Assert.assertNotNull(sips.getFeatures().size() == 1);
        Assert.assertTrue(sips.getFeatures().get(0) instanceof SIP);
        SIP one = sips.getFeatures().get(0);
        SIPProperties ppties = one.getProperties();
        Assert.assertNotNull(ppties);
        Assert.assertTrue(ppties.getDataObjects().size() == 1);
        SIPDataObject dataObject = ppties.getDataObjects().get(0);
        Assert.assertEquals(url, dataObject.getUrl());
        Assert.assertEquals(mimeType, dataObject.getMimeType());
        Assert.assertEquals(dataType, dataObject.getDataType());
        Assert.assertEquals(checksum, dataObject.getChecksum());
        Assert.assertEquals(checksumAlgorithm, dataObject.getChecksumAlgorithm());
    }
}
