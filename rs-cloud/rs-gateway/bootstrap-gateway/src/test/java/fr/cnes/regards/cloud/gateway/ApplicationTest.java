/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 *
 * Class ApplicationTest
 *
 * Spring Context test
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@SpringBootTest(classes = Application.class)
public class ApplicationTest {

    @Test
    public void initContext() {
        // Nothing to do. Only check Spring context validity
    }

}
