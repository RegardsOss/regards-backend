/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;

import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;

/**
 *
 * Class ApplicationTest
 *
 * Spring Context test
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RunWith(RegardsSpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ApplicationTest {

    @Test
    public void initContext() {
        // Nothing to do. Only check Spring context validity
    }

}
