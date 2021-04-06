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
package fr.cnes.regards.modules.toponyms;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.toponyms.dao.ToponymsRepository;
import fr.cnes.regards.modules.toponyms.domain.ToponymGeoJson;
import fr.cnes.regards.modules.toponyms.domain.ToponymsRestConfiguration;
import fr.cnes.regards.modules.toponyms.service.ToponymsService;
import fr.cnes.regards.modules.toponyms.service.exceptions.GeometryNotParsedException;
import fr.cnes.regards.modules.toponyms.service.exceptions.MaxLimitPerDayException;
import java.io.IOException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author Sébastien Binda
 */
@TestPropertySource(properties = {"spring.jpa.properties.hibernate.default_schema=toponym_controller_it",
        "regards.toponyms.limit.save=2"})
@RegardsTransactional
public class ToponymControllerIT extends AbstractRegardsTransactionalIT {


    @Autowired
    private ToponymsService toponymsService;

    @Autowired
    private ToponymsRepository repository;

    @Value("${regards.toponyms.limit.save}")
    private int maxLimit;


    private static final String TEST_USER = "test_user";

    private static final String TEST_PROJECT = "test_project";

    private final static String LINESTRING = "{\"type\": \"Feature\", \"properties\": {\"test\": 546169.05592760979},\"geometry\": {\"type\": \"LineString\",\"coordinates\": [ [ 6.199999999999898, 7.895833333333167], [ 6.229166666666564, 7.89583333333316], [ 6.262499999999898, 7.862499999999832]] }}";

    private final static String POLYGON = "{\"type\": \"Feature\", \"properties\": {\"test\" : 42}, \"geometry\": { \"type\": \"Polygon\", \"coordinates\": [[ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]] }}";

    private final static String MULTIPOLYGON = "{\"type\": \"Feature\", \"properties\": {\"test\" : 42}, \"geometry\": { \"type\": \"MultiPolygon\", \"coordinates\": [" +
            "[[[102.0, 2.0], [103.0, 2.0], [103.0, 3.0], [102.0, 3.0], [102.0, 2.0]]]," +
            "[[[100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0]]," +
            "[[100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2]]]" +
            "]}}";


    @Test
    public void findAll() {
        performDefaultGet(ToponymsRestConfiguration.ROOT_MAPPING, customizer().expectStatusOk()
                        .expectToHaveSize(JSON_PATH_CONTENT, 10).addParameter("page", "0").addParameter("size", "10"),
                "should be  ok");
    }

    @Test
    public void search() {
        performDefaultGet(ToponymsRestConfiguration.ROOT_MAPPING + ToponymsRestConfiguration.SEARCH,
                customizer().expectStatusOk().addParameter("locale", "en"), "should be  ok");
    }

    @Test
    public void findOne() {
        performDefaultGet(ToponymsRestConfiguration.ROOT_MAPPING + ToponymsRestConfiguration.TOPONYM_ID,
                customizer().expectStatusOk().expectDoesNotExist("$.content.toponymMetadata.expirationDate"),
                "Martinique toponym should be retried", "Martinique");

        performDefaultGet(ToponymsRestConfiguration.ROOT_MAPPING + ToponymsRestConfiguration.TOPONYM_ID,
                customizer().expectStatus(HttpStatus.NOT_FOUND), "Somewhere toponym should not exists",
                "Somewhere");
    }

    @Test
    public void testConf() {
/*        performDefaultGet(ToponymsRestConfiguration.ROOT_MAPPING, customizer().expectStatusOk()
                                  .expectToHaveSize(JSON_PATH_CONTENT, 10).addParameter("page", "0").addParameter("size", "10"),
                          "should be  ok")*/
    }

    @Test
    @Purpose("Test the creation of a not visible toponym with a type not handled by REGARDS")
    @ExceptionHandler(GeometryNotParsedException.class)
    public void createInvalidNotVisibleToponym() throws IOException {
        performDefaultPost(ToponymsRestConfiguration.ROOT_MAPPING, new ToponymGeoJson(LINESTRING, TEST_USER, TEST_PROJECT),
                customizer().expectStatus(HttpStatus.BAD_REQUEST), "Should not have created toponym");
    }

    @Test
    @Purpose("Test the successful creation of visible toponyms handled by REGARDS")
    public void createValidNotVisibleToponyms() {
        performDefaultPost(ToponymsRestConfiguration.ROOT_MAPPING, new ToponymGeoJson(POLYGON, TEST_USER, TEST_PROJECT), customizer().expectStatus(HttpStatus.CREATED), "Should have created toponym");
        performDefaultPost(ToponymsRestConfiguration.ROOT_MAPPING, new ToponymGeoJson(MULTIPOLYGON, TEST_USER, TEST_PROJECT), customizer().expectStatus(HttpStatus.CREATED), "Should have created toponym");

    }

    @Test
    @Purpose("Test limit of created toponyms reached for the day")
    @ExceptionHandler(MaxLimitPerDayException.class)
    public void testLimitToponymsSaving() {
        performDefaultPost(ToponymsRestConfiguration.ROOT_MAPPING, new ToponymGeoJson(POLYGON, TEST_USER, TEST_PROJECT), customizer().expectStatus(HttpStatus.CREATED), "Should have created toponym");
        performDefaultPost(ToponymsRestConfiguration.ROOT_MAPPING, new ToponymGeoJson(POLYGON, TEST_USER, TEST_PROJECT), customizer().expectStatus(HttpStatus.CREATED), "Should have created toponym");
        performDefaultPost(ToponymsRestConfiguration.ROOT_MAPPING, new ToponymGeoJson(POLYGON, TEST_USER, TEST_PROJECT), customizer().expectStatus(HttpStatus.FORBIDDEN), "Should have created toponym");

    }
}
