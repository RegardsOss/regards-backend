package fr.cnes.regards.cloud.registry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import fr.cnes.regards.cloud.registry.Application;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = Application.class)
public class RegistryApplicationTest {

    @Test
    public void contextLoads() {
        // The application can start with spring configuration
    }

}
