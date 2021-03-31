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

import fr.cnes.regards.framework.geojson.geometry.MultiPolygon;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.toponyms.dao.ToponymsRepository;
import fr.cnes.regards.modules.toponyms.domain.Toponym;
import fr.cnes.regards.modules.toponyms.domain.ToponymDTO;
import fr.cnes.regards.modules.toponyms.domain.ToponymLocaleEnum;
import fr.cnes.regards.modules.toponyms.domain.ToponymMetadata;
import fr.cnes.regards.modules.toponyms.service.exceptions.GeometryNotParsedException;
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
        Assert.assertEquals("Without simplify algorithm france should contains 1042 positions", 1042, size);
    }

    @Test
    @Purpose("Check if expirationDate is updated only for not visible toponyms")
    public void findToponym() {
        Optional<ToponymDTO> visibleToponym = service.findOne("France", false);
        Assert.assertTrue(String.format("Toponym %s should be present", visibleToponym), visibleToponym.isPresent());
        Assert.assertTrue("expirationDate of a visible toponym should always be empty", visibleToponym.get().getToponymMetadata().getExpirationDate() == null);

        // Tested not visible toponym
        OffsetDateTime oldDateTime = this.temporaryToponyms.get(0).getToponymMetadata().getExpirationDate();
        Optional<ToponymDTO> notVisibleToponym = service.findOne(this.temporaryToponyms.get(0).getBusinessId(), false);
        Assert.assertTrue(String.format("Toponym %s should be present", notVisibleToponym), notVisibleToponym.isPresent());
        Assert.assertEquals("expirationDate should have been updated", oldDateTime.plusDays(this.defaultExpiration)
                , notVisibleToponym.get().getToponymMetadata().getExpirationDate());

    }

    @Test
    @Purpose("Parse valid and handled geometry")
    public void parseValidGeometry() throws ModuleException {
        String polygon = "{\"type\": \"Feature\", \"properties\": {\"test\" : 42}, \"geometry\": { \"type\": \"Polygon\", \"coordinates\": [[ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]] }}";
        String multipolygon = "{\"type\": \"Feature\", \"properties\": {\"test\" : 42}, \"geometry\": { \"type\": \"MultiPolygon\", \"coordinates\": [" +
                "[[[102.0, 2.0], [103.0, 2.0], [103.0, 3.0], [102.0, 3.0], [102.0, 2.0]]]," +
                "[[[100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0]]," +
                "[[100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2]]]" +
                "]}}";
        this.service.generateNotVisibleToponym(polygon, "test_user", "test_project");
        this.service.generateNotVisibleToponym(multipolygon, "test_user", "test_project");
    }

    @Test(expected = GeometryNotParsedException.class)
    @Purpose("Parse invalid geometry")
    public void parseInvalidGeometry() throws ModuleException {
        String invalidFeature = "{{\"type\": \"Feature\", \"properties\": {\"test\": 546169.05592760979}, \"geometry\": {\"type\": \"LineString\",\"coordinates\": []}}";
        this.service.generateNotVisibleToponym(invalidFeature, "test_user", "test_project");
    }

    @Test(expected = GeometryNotParsedException.class)
    @Purpose("Parse not handled geometry")
    public void parseNotHandledGeometry() throws ModuleException {
        String invalidFeature = "{\"type\": \"Feature\", \"properties\": {\"test\": 546169.05592760979}, \"geometry\": {\"type\": \"LineString\",\"coordinates\": []}}";
        this.service.generateNotVisibleToponym(invalidFeature, "test_user", "test_project");
    }


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
