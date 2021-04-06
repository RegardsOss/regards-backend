package fr.cnes.regards.modules.backendforfrontend.rest;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.toponyms.domain.ToponymGeoJson;
import fr.cnes.regards.modules.toponyms.domain.ToponymsRestConfiguration;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration Test for {@link ToponymsController}
 *
 * @author Iliana Ghazali
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=frontend_toponyms_controller"})
public class ToponymControllerIT extends AbstractRegardsIT {

    private static final String TEST_USER = "test_user";

    private static final String TEST_PROJECT = "test_project";

    private final static String POLYGON = "{\"type\": \"Feature\", \"properties\": {\"test\" : 42}, \"geometry\": { \"type\": \"Polygon\", \"coordinates\": [[ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]] }}";


    @Test
    @Purpose("Test the creation of a not visible toponym with a type not handled by REGARDS")
    public void createInvalidNotVisibleToponym() {
        performDefaultPost(ToponymsRestConfiguration.ROOT_MAPPING, new ToponymGeoJson(POLYGON, TEST_USER, TEST_PROJECT),
                customizer().expectStatus(HttpStatus.OK), "Should have created toponym");
    }



}
