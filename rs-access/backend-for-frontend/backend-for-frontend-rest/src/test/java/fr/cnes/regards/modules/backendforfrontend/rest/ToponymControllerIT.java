package fr.cnes.regards.modules.backendforfrontend.rest;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.toponyms.domain.ToponymsRestConfiguration;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration Test for {@link ToponymsController}
 *
 * @author Iliana Ghazali
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=frontend_toponyms_controller" })
public class ToponymControllerIT extends AbstractRegardsIT {


    @Test
    @Purpose("Test post of toponym")
    public void testLimitToponymsSaving() {
        String toponym = "{\"toponym\":\"{\\\"type\\\":\\\"Feature\\\",\\\"properties\\\":{\\\"id_map\\\":0,\\\"id_zone\\\":1,\\\"id_gaspar\\\":\\\"44DDTM20140031\\\",\\\"cote_ref\\\":5,\\\"urlfic\\\":\\\"http://www.loire-atlantique.gouv.fr/Politiques-publiques/Risques-naturels-et-technologiques/Prevention-des-risques-naturels/Plans-Prevention-Risques-Naturels-Previsibles/Les-Plans-de-Prevention-des-Risques-Littoraux-en-Loire-Atlantique\\\",\\\"nomfic\\\":\\\"Le-PPRL-Cote-de-Jade\\\"},\\\"geometry\\\":{\\\"type\\\":\\\"Polygon\\\",\\\"coordinates\\\":[[[-2.170333553566122,47.25914934169389],[-2.170321658948729,47.25914987267915],[-2.1703136074917,47.25906627958832],[-2.170325502090678,47.25906574860383],[-2.170333553566122,47.25914934169389]]]}}\"}";
        performDefaultPost(ToponymsRestConfiguration.ROOT_MAPPING, toponym, customizer().expectStatus(HttpStatus.OK), "Should have created toponym");
    }

}
