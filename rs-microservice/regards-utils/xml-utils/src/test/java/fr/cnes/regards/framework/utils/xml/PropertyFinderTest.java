/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.xml;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.utils.xml.xpath.PropertyFinder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Stephane Cortine
 **/
public class PropertyFinderTest {

    private static final Path GOLDEN_DATASET = Paths.get("src", "test", "resources", "golden-dataset");

    private static final String GOLDEN_DATA_FILE = "SWOT_L1B_HR_SLC_003_096_212L_20220722T090107_20220722T090137_PG99_01.nc.iso.xml";

    private PropertyFinder finder;

    @Before
    public void setUp() throws Exception {
        finder = new PropertyFinder();
    }

    @Test
    public void test_extract_node()
        throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        // Given
        String xPath = "/gmi:MI_Metadata/gmd:fileIdentifier/gco:CharacterString";

        List<String> result = null;

        // When
        try (InputStream is = Files.newInputStream(GOLDEN_DATASET.resolve(GOLDEN_DATA_FILE))) {
            result = finder.extractTextByXPath(is, xPath, true);
        }

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("SWOT_L1B_HR_SLC_003_096_212L_20220722T090107_20220722T090137_PG99_01.nc", result.get(0));
    }

    @Test
    public void test_extract_two_nodes()
        throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        // Given
        String xPath = "//gmd:language/gco:CharacterString";

        List<String> result = null;

        // When
        try (InputStream is = Files.newInputStream(GOLDEN_DATASET.resolve(GOLDEN_DATA_FILE))) {
            result = finder.extractTextByXPath(is, xPath, true);
        }

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("eng", result.get(0));
        Assert.assertEquals("eng", result.get(1));
    }

    @Test
    public void test_extract_several_nodes()
        throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        // Given
        String xPath = "//gco:CharacterString";

        List<String> result = null;

        // When
        try (InputStream is = Files.newInputStream(GOLDEN_DATASET.resolve(GOLDEN_DATA_FILE))) {
            result = finder.extractTextByXPath(is, xPath, true);
        }

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(109, result.size());
        Assert.assertEquals("SWOT_L1B_HR_SLC_003_096_212L_20220722T090107_20220722T090137_PG99_01.nc", result.get(0));
        Assert.assertEquals("eng", result.get(1));
    }

    @Test
    public void test_extract_several_nodes_function_dateTime()
        throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        // Given
        String xPath = "h2:datetime(//gco:DateTime)";

        List<String> result = null;

        // When
        try (InputStream is = Files.newInputStream(GOLDEN_DATASET.resolve(GOLDEN_DATA_FILE))) {
            result = finder.extractTextByXPath(is, xPath, true);
        }

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        Assert.assertTrue(result.contains("2018-04-01T00:00Z"));
        Assert.assertTrue(result.contains("2018-04-02T00:00Z"));
        Assert.assertTrue(result.contains("2018-04-03T00:00Z"));
    }

    @Test
    public void test_extract_node_function_bbox()
        throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        // Given
        String xPath = "h2:bbox(//gmd:EX_GeographicBoundingBox)";

        List<String> result = null;

        // When
        try (InputStream is = Files.newInputStream(GOLDEN_DATASET.resolve(GOLDEN_DATA_FILE))) {
            result = finder.extractTextByXPath(is, xPath, true);
        }

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.contains(
            "[-129.13090393992763,5.266971710251425,-128.46876391015957,5.6494921501577515]"));
    }

    @Test
    public void test_extract_two_nodes_attribut()
        throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        // Given
        String xPath = "//gmd:MD_ScopeCode/@codeList";

        List<String> result = null;

        // When
        try (InputStream is = Files.newInputStream(GOLDEN_DATASET.resolve(GOLDEN_DATA_FILE))) {
            result = finder.extractTextByXPath(is, xPath, true);
        }

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(
            "https://cdn.earthdata.nasa.gov/iso/resources/Codelist/gmxCodelists.xml#MD_ScopeCode"));
        Assert.assertTrue(result.contains(
            "http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#MD_ScopeCode"));
    }

    @Test
    public void test_extract_multiple_IGeometry()
        throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        // Given
        String xPath = "//gmd:EX_BoundingPolygon";
        // When
        List<IGeometry> result = extract_IGeometry_without_sampling(xPath);

        // Then
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() == 3);
        Assert.assertEquals(result.get(0), GeometryFactory.createPoint());
        Assert.assertEquals(result.get(1), GeometryFactory.createLineString());
        Assert.assertEquals(result.get(2), GeometryFactory.createPolygon());
    }

    @Test
    public void test_extract_IGeometry_Polygon()
        throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        // Given
        String xPath = "//gml:Polygon";
        // When
        List<IGeometry> result = extract_IGeometry_without_sampling(xPath);

        // Then
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() == 1);
        Assert.assertEquals(result.get(0), GeometryFactory.createPolygon());
    }

    private List<IGeometry> extract_IGeometry_without_sampling(String xpath)
        throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        // Given
        String xPath = xpath;

        List<IGeometry> result = null;

        // When
        try (InputStream is = Files.newInputStream(GOLDEN_DATASET.resolve(GOLDEN_DATA_FILE))) {
            result = finder.extractGeometryByXPath(is, xPath, 0, true);
        }
        return result;
    }

    @Test
    public void test_extract_multiple_IGeometry_with_sampling_10()
        throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        // Given
        String xPath = "//gmd:EX_BoundingPolygon";
        int pointSampling = 10;

        List<IGeometry> result = null;

        // When
        try (InputStream is = Files.newInputStream(GOLDEN_DATASET.resolve(GOLDEN_DATA_FILE))) {
            result = finder.extractGeometryByXPath(is, xPath, pointSampling, true);
        }

        // Then
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() == 3);
        Assert.assertEquals(result.get(0), GeometryFactory.createPoint());
        Assert.assertEquals(result.get(1), GeometryFactory.createLineString());
        Assert.assertEquals(result.get(2), GeometryFactory.createPolygon_with_sampling_10());
    }

    @Test
    public void test_extract_IGeometry_Polygon_with_sampling_10()
        throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        // Given
        String xPath = "//gml:Polygon";
        int pointSampling = 10;
        String xmlTestFile = "polygon.xml";

        List<IGeometry> result = null;

        // When
        try (InputStream is = Files.newInputStream(GOLDEN_DATASET.resolve(xmlTestFile))) {
            result = finder.extractGeometryByXPath(is, xPath, pointSampling, true);
        }

        // Then
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() == 1);
        Assert.assertEquals(result.get(0), GeometryFactory.createPolygon_with_sampling_10());
    }

}
