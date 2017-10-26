package fr.cnes.regards.microservices.storage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;

@SpringBootTest
@EnableAutoConfiguration
@RunWith(SpringRunner.class)
@Import(ConfigurationTest.class)
public class ApplicationTest extends AbstractRegardsIT {

    @Test
    public void applicationTest() {

    }

}
