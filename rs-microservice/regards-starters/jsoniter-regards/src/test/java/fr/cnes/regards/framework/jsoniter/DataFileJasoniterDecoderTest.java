/*

 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.jsoniter;

import com.google.gson.Gson;
import com.jsoniter.JsonIterator;
import com.jsoniter.spi.JsoniterSpi;
import fr.cnes.regards.framework.jsoniter.decoders.DataFileJsoniterDecoder;
import fr.cnes.regards.framework.jsoniter.property.JsoniterAttributeModelPropertyTypeFinder;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Stephane Cortine
 **/
public class DataFileJasoniterDecoderTest {

    @Before
    public void setUp() {
        // Register all decoders before tests
        JsoniterSpi.setCurrentConfig(new IIndexableJsoniterConfig());
        new JsoniterDecoderRegisterer(new JsoniterAttributeModelPropertyTypeFinder(), new Gson());
    }

    @Test
    public void parseDataFileTest() throws Exception {
        String content = readResource("datafile.json");

        DataFileJsoniterDecoder decoder = new DataFileJsoniterDecoder();
        Object parsed = decoder.decode(JsonIterator.parse(content));

        if (parsed instanceof DataFile dataFile) {
            Assertions.assertEquals(
                "http://vm-perf.cloud-espace.si.c-s.fr:80/api/v1/rs-catalog/downloads/URN:AIP:DATA:perf:35a8b1aa-7d90-3f34-bc94-646424f8cee3:V1/files/7074d502c18f4a0f9e595442ef040d3a",
                dataFile.getUri(),
                "Bad uri");
            Assertions.assertEquals("Haut-Rhin.png", dataFile.getFilename(), "Bad filename");
            Assertions.assertEquals("THUMBNAIL", dataFile.getDataType().toString(), "Bad dataType");
            Assertions.assertEquals(10021, dataFile.getFilesize(), "Bad filesize");
            Assertions.assertTrue(dataFile.isOnline(), "Bad online");
            Assertions.assertFalse(dataFile.isReference(), "Bad reference");
            Assertions.assertEquals("image/png", dataFile.getMimeType().toString(), "Bad mimeType");
            Assertions.assertEquals("7074d502c18f4a0f9e595442ef040d3a", dataFile.getChecksum(), "Bad checksum");
            Assertions.assertEquals("MD5", dataFile.getDigestAlgorithm(), "Bad digestAlgorithm");
            Assertions.assertEquals(297.0, dataFile.getImageHeight(), "Bad imageHeight");
            Assertions.assertEquals(170.0, dataFile.getImageWidth(), "Bad imageWidth");
            Assertions.assertEquals("d87f7e0c", dataFile.getCrc32(), "Bad crc32");

            Assertions.assertEquals(1, dataFile.getTypes().size(), "Bad types");
            Assertions.assertTrue(dataFile.getTypes().contains("type1"), "Bad type1");
            Assertions.assertTrue(dataFile.getAdditionalFields().toString().startsWith("{"),
                                  "Bad additional fields : must start with { ");
            Assertions.assertNotNull(dataFile.getAdditionalFields(), "Bad associated fields");
            Assertions.assertTrue(dataFile.getAdditionalFields().toString().contains("value1"),
                                  "Bad additional fields : value1 is not contained : " + dataFile.getAdditionalFields()
                                                                                                 .toString());
        } else {
            Assertions.fail("Should be able to deserialize as DataFile");
        }
    }

    @Test
    public void parseDataFile2Test() throws Exception {
        String content = readResource("datafile2.json");

        DataFileJsoniterDecoder decoder = new DataFileJsoniterDecoder();
        Object parsed = decoder.decode(JsonIterator.parse(content));

        if (parsed instanceof DataFile dataFile) {
            Assertions.assertEquals(
                "http://vm-perf.cloud-espace.si.c-s.fr:80/api/v1/rs-catalog/downloads/URN:AIP:DATA:perf:35a8b1aa-7d90-3f34-bc94-646424f8cee3:V1/files/7074d502c18f4a0f9e595442ef040d3a",
                dataFile.getUri(),
                "Bad uri");
            Assertions.assertEquals("Bas-Rhin.png", dataFile.getFilename(), "Bad filename");
            Assertions.assertEquals(DataType.RAWDATA, dataFile.getDataType(), "Bad dataType");
            Assertions.assertEquals(10021, dataFile.getFilesize(), "Bad filesize");
            Assertions.assertTrue(dataFile.isOnline(), "Bad online");
            Assertions.assertFalse(dataFile.isReference(), "Bad reference");
            Assertions.assertEquals("image/png", dataFile.getMimeType().toString(), "Bad mimeType");
            Assertions.assertEquals("7074d502c18f4a0f9e595442ef040d3a", dataFile.getChecksum(), "Bad checksum");
            Assertions.assertEquals("MD5", dataFile.getDigestAlgorithm(), "Bad digestAlgorithm");
            Assertions.assertEquals(297.0, dataFile.getImageHeight(), "Bad imageHeight");
            Assertions.assertEquals(170.0, dataFile.getImageWidth(), "Bad imageWidth");
            Assertions.assertEquals("d87f7e0c", dataFile.getCrc32(), "Bad crc32");

            Assertions.assertEquals(2, dataFile.getTypes().size(), "Bad types");
            Assertions.assertTrue(dataFile.getTypes().contains("type1"), "Bad type1");
            Assertions.assertTrue(dataFile.getTypes().contains("type2"), "Bad type1");
            Assertions.assertNotNull(dataFile.getAdditionalFields(), "Bad associated fields");
            Assertions.assertTrue(dataFile.getAdditionalFields().toString().startsWith("["),
                                  "Bad additional fields : must start with [ ");
            Assertions.assertTrue(dataFile.getAdditionalFields().toString().contains("value1"),
                                  "Bad additional fields : value1 is not contained : " + dataFile.getAdditionalFields()
                                                                                                 .toString());
            Assertions.assertTrue(dataFile.getAdditionalFields().toString().contains("value2"),
                                  "Bad additional fields : value2 is not contained : " + dataFile.getAdditionalFields()
                                                                                                 .toString());
        } else {
            Assertions.fail("Should be able to deserialize as DataFile");
        }
    }

    private String readResource(String s) throws IOException {
        InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(s);
        return IOUtils.toString(in, StandardCharsets.UTF_8);
    }
}
