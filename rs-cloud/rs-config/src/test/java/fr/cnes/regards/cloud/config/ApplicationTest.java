/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * Class ConfigApplicationTest
 *
 * Test class for Srping Config Server.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ApplicationTest {

    /**
     *
     * Test that the spring context is well loaded
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void contextLoads() {
        // The application can start with spring configuration
    }

}