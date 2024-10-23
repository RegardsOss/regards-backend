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

package fr.cnes.regards.modules.opensearch.service;

import fr.cnes.regards.framework.geojson.geometry.MultiPolygon;
import fr.cnes.regards.framework.geojson.geometry.Polygon;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.OrCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.PolygonCriterion;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;
import fr.cnes.regards.modules.opensearch.service.parser.ToponymParser;
import fr.cnes.regards.modules.toponyms.client.IToponymsClient;
import fr.cnes.regards.modules.toponyms.domain.ToponymDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ToponymParser}
 *
 * @author Iliana Ghazali
 */

@TestPropertySource(locations = "classpath:test.properties")
public class ToponymParserIT extends AbstractRegardsTransactionalIT {

    @Autowired
    IToponymsClient toponymClient;

    public ToponymParser parser;

    @Before
    public void init() {
        parser = new ToponymParser(toponymClient);
    }

    @Test
    @Purpose("Test that a criterion is returned from a polygon parameter ")
    public void testRetrievePolygon() throws UnsupportedEncodingException, OpenSearchParseException {
        // Initialize polygon
        Polygon polygon = Polygon.fromArray(generatePolygon(1, 1, 5));
        ToponymDTO toponym = ToponymDTO.build("test", "test", "test", polygon, "test", "test", false, "", null);

        // Build toponym mock
        when(toponymClient.get(anyString())).thenReturn(new ResponseEntity<>(EntityModel.of(toponym), HttpStatus.OK));

        // Test parsing function
        String request = URLEncoder.encode("id", "UTF-8");
        ICriterion criterion = parser.parse(ToponymParser.TOPONYM_BUSINESS_ID + "=" + request);

        // Check results
        Assert.assertNotNull(criterion);
        Assert.assertTrue(criterion instanceof PolygonCriterion);
        checkPolygonCoordinates((PolygonCriterion) criterion, polygon.toArray());
    }

    @Test
    @Purpose("Test that a criterion is returned from a multi-polygon parameter ")
    public void testRetrieveMultiPolygon() throws UnsupportedEncodingException, OpenSearchParseException {
        // Initialize multipolygon
        int nbPolygons = 2;
        double[][][][] multiPolygonArray = new double[nbPolygons][][][];
        multiPolygonArray[0] = generatePolygon(1, 1, 3);
        multiPolygonArray[1] = generatePolygon(10, 2, 4);
        MultiPolygon multiPolygon = MultiPolygon.fromArray(multiPolygonArray);
        ToponymDTO toponym = ToponymDTO.build("test", "test", "test", multiPolygon, "test", "test", false, "", null);

        // Build toponym mock
        when(toponymClient.get(anyString())).thenReturn(new ResponseEntity<>(EntityModel.of(toponym), HttpStatus.OK));

        // Test parsing function
        String request = URLEncoder.encode("id", "UTF-8");
        ICriterion multiCriterion = parser.parse(ToponymParser.TOPONYM_BUSINESS_ID + "=" + request);

        // Check results
        Assert.assertNotNull(multiCriterion);
        Assert.assertTrue(multiCriterion instanceof OrCriterion);
        List<ICriterion> listCriterion = ((OrCriterion) multiCriterion).getCriterions();
        Assert.assertEquals(String.format("Expected %s polygons", nbPolygons), nbPolygons, listCriterion.size());
        for (int i = 0; i < nbPolygons; i++) {
            ICriterion criterion = listCriterion.get(i);
            Assert.assertTrue(criterion instanceof PolygonCriterion);
            checkPolygonCoordinates((PolygonCriterion) criterion, multiPolygonArray[i]);
        }
    }

    public double[][][] generatePolygon(int startIndex, int nbInteriorRings, int nbPointsPerRing) {
        double[][][] polygon = new double[nbInteriorRings + 1][][];

        for (int ring = 0; ring < (nbInteriorRings + 1); ring++) {
            polygon[ring] = new double[nbPointsPerRing][2];
            for (int i = 0; i < (nbPointsPerRing - 1); i++) {
                polygon[ring][i][0] = i + startIndex;
                polygon[ring][i][1] = i + startIndex + 1;
            }
            polygon[ring][nbPointsPerRing - 1][0] = polygon[ring][0][0];
            polygon[ring][nbPointsPerRing - 1][1] = polygon[ring][0][1];
        }
        return polygon;
    }

    public void checkPolygonCoordinates(PolygonCriterion polygonCriterion, double[][][] expectedPolygon) {
        Assert.assertTrue("Arrays should have been equal",
                          Arrays.deepEquals(polygonCriterion.getCoordinates(), expectedPolygon));

    }

}
