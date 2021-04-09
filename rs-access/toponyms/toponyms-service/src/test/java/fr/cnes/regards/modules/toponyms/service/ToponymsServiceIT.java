/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.toponyms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.geojson.geometry.MultiPolygon;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.toponyms.dao.ToponymsRepository;
import fr.cnes.regards.modules.toponyms.domain.Toponym;
import fr.cnes.regards.modules.toponyms.domain.ToponymDTO;
import fr.cnes.regards.modules.toponyms.domain.ToponymLocaleEnum;
import fr.cnes.regards.modules.toponyms.domain.ToponymMetadata;
import fr.cnes.regards.modules.toponyms.service.exceptions.GeometryNotHandledException;
import fr.cnes.regards.modules.toponyms.service.exceptions.GeometryNotProcessedException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/**
 *
 * @author Sébastien Binda
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=toponyms_service_it", "regards.toponyms.expiration=30"})
@RegardsTransactional
public class ToponymsServiceIT extends AbstractRegardsIT {

    @Autowired
    private ToponymsService service;

    @Autowired
    private ToponymsRepository toponymRepo;

    @Value("${regards.toponyms.expiration}")
    private int defaultExpiration;

    private final String LOCALE = ToponymLocaleEnum.EN.getLocale();

    private final boolean TOPONYM_VISIBILITY = true;

    private List<Toponym> temporaryToponyms;

    @Before
    public void init() {
        // init temporary toponyms
        this.temporaryToponyms = initNotVisibleToponyms();
    }

    @Test
    public void findAllByVisibility() throws IOException, ModuleException, URISyntaxException {
        Page<ToponymDTO> results = service.findAllByVisibility(LOCALE, TOPONYM_VISIBILITY, PageRequest.of(0, 100));
        Assert.assertEquals(251, results.getTotalElements());
        Assert.assertEquals(100, results.getSize());
    }

    @Test
    public void search() {
        List<ToponymDTO> toponyms = service.search("Fran", "en", TOPONYM_VISIBILITY, 100);
        Assert.assertEquals(1, toponyms.size());

        toponyms = service.search("fr", "en", TOPONYM_VISIBILITY, 100);
        Assert.assertEquals(6, toponyms.size());

        toponyms = service.search("e", "en", TOPONYM_VISIBILITY, 100);
        Assert.assertEquals(100, toponyms.size());
    }

    @Test
    public void searchSimplifiedGeo() {
        Optional<ToponymDTO> result = service.findOne("France", true);
        int size = ((MultiPolygon) result.get().getGeometry()).getCoordinates().stream()
                .map(c -> c.stream().map(p -> p.size()).reduce(0, Integer::sum)).reduce(0, Integer::sum);
        Assert.assertEquals("With simplify algorithm france should contains 141 positions", 141, size);

        result = service.findOne("France", false);
        size = ((MultiPolygon) result.get().getGeometry()).getCoordinates().stream()
                .map(c -> c.stream().map(p -> p.size()).reduce(0, Integer::sum)).reduce(0, Integer::sum);
        Assert.assertEquals("Without simplify algorithm france should contains 1042 positions", 141, size);
    }

    @Test
    @Purpose("Check if expirationDate is updated only for not visible toponyms")
    public void findToponym() {
        // Test expiration date of visible toponym
        Optional<ToponymDTO> visibleToponym = service.findOne("France", false);
        Assert.assertTrue(String.format("Toponym %s should be present", visibleToponym), visibleToponym.isPresent());
        Assert.assertTrue("expirationDate of a visible toponym should always be empty", visibleToponym.get().getToponymMetadata().getExpirationDate() == null);

        // Test expiration date of not visible toponym
        OffsetDateTime oldDateTime = this.temporaryToponyms.get(0).getToponymMetadata().getExpirationDate();
        Optional<ToponymDTO> notVisibleToponym = service.findOne(this.temporaryToponyms.get(0).getBusinessId(), false);
        Assert.assertTrue(String.format("Toponym %s should be present", notVisibleToponym), notVisibleToponym.isPresent());
        Assert.assertNotEquals("expirationDate should have been updated", oldDateTime
                , notVisibleToponym.get().getToponymMetadata().getExpirationDate());

    }

    // -----------------------------
    // --- CREATE VALID TOPONYMS ---
    // -----------------------------

    @Test
    @Purpose("Parse valid and handled geometry")
    public void parseValidFeature() throws ModuleException, JsonProcessingException {
        // Init
        String polygon = "{\"type\": \"Feature\", \"properties\": {\"test\" : 42}, \"geometry\": { \"type\": \"Polygon\", \"coordinates\": [[ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]] }}";
        String multipolygon = "{\"type\": \"Feature\", \"properties\": {\"test\" : 42}, \"geometry\": { \"type\": \"MultiPolygon\", \"coordinates\": [" +
                "[[[102.0, 2.0], [103.0, 2.0], [103.0, 3.0], [102.0, 3.0], [102.0, 2.0]]]," +
                "[[[100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0]]," +
                "[[100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2]]]" +
                "]}}";
        ToponymDTO polygonToponym = this.service.generateNotVisibleToponym(polygon, "test_user", "test_project");
        ToponymDTO multiPolygonToponym = this.service.generateNotVisibleToponym(multipolygon, "test_user", "test_project");

        // Test result
        Assert.assertTrue("Geometry should be present and of type Polygon", polygonToponym.getGeometry() != null && polygonToponym.getGeometry().getType().equals(GeoJsonType.POLYGON));
        Assert.assertTrue("Multipolygon Geometry should be present and of type Multipolygon", multiPolygonToponym.getGeometry() != null && multiPolygonToponym.getGeometry().getType().equals(GeoJsonType.MULTIPOLYGON));

    }

    // -------------------------------
    // --- CREATE INVALID TOPONYMS ---
    // -------------------------------

    @Test(expected = JsonProcessingException.class)
    @Purpose("Parse invalid feature. The json is malformed.")
    public void parseInvalidFeature() throws ModuleException, JsonProcessingException {
        String invalidFeature = "{{\"type\": \"Feature\", \"properties\": {\"test\": 546169.05592760979}, \"geometry\": {\"type\": \"Polygon\",\"coordinates\": []}}";
        this.service.generateNotVisibleToponym(invalidFeature, "test_user", "test_project");
    }

    @Test(expected = GeometryNotProcessedException.class)
    @Purpose("Parse invalid feature. The fields are not present as expected (refer to geolatte Feature)")
    public void parseInvalidFormatFeature() throws ModuleException, JsonProcessingException {
        String invalidFeature = "{\"type\": \"Random\", \"properties\": {\"test\" : 42}, \"object\": { \"type\": \"Polygon\", \"coordinates\": [[ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]] }}";
        this.service.generateNotVisibleToponym(invalidFeature, "test_user", "test_project");
    }

    @Test(expected = GeometryNotHandledException.class)
    @Purpose("Parse not handled geometry (LineString)")
    public void parseNotHandledGeometry() throws ModuleException, JsonProcessingException {
        String invalidFeature = "{\"type\": \"Feature\", \"properties\": {\"test\": 546169.05592760979}, \"geometry\": {\"type\": \"LineString\",\"coordinates\": []}}";
        this.service.generateNotVisibleToponym(invalidFeature, "test_user", "test_project");
    }

    @Test(expected = EntityAlreadyExistsException.class)
    @Purpose("Parse already existing geometry")
    public void parseAlreadyExistingGeometry() throws ModuleException, JsonProcessingException {
        String polygon = "{\"type\": \"Feature\", \"properties\": {\"test\" : 42}, \"geometry\": { \"type\": \"Polygon\", \"coordinates\": [[ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]] }}";
        this.service.generateNotVisibleToponym(polygon, "test_user", "test_project");
        this.service.generateNotVisibleToponym(polygon, "test_user", "test_project");
    }

    // -------------------------------
    // ------------ UTILS ------------
    // -------------------------------

    private List<Toponym> initNotVisibleToponyms() {
        List<Toponym> notVisibleToponyms = new ArrayList<>();
        int nbToponyms = 10;
        for (int i = 0; i < nbToponyms; i++) {
            String name = "ToponymTest " + i;
            OffsetDateTime currentDateTime = OffsetDateTime.now();
            ToponymMetadata metadata = new ToponymMetadata(currentDateTime, currentDateTime.plusDays(this.defaultExpiration), "test_user", "test_project");
            notVisibleToponyms.add(new Toponym(name, name, name, null, null, null, false, metadata));
        }
        return this.toponymRepo.saveAll(notVisibleToponyms);
    }
}
