package fr.cnes.regards.cloud.registry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 *
 * Class RegistryApplicationTest
 *
 * Configuration class for spring context tests
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = Application.class)
public class RegistryApplicationTest {

    /**
     *
     * Check that the Spring context is well loaded at startup
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void contextLoads() {
        // The application can start with spring configuration
    }

}
