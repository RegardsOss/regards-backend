/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.sequencegen;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.sequencegen.SequenceGeneratorTest.SequenceGeneratorTestConfiguration;

/**
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { SequenceGeneratorTestConfiguration.class })
@ActiveProfiles("dev")
@TestPropertySource("/seqgen.properties")
public class SequenceGeneratorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SequenceGeneratorTest.class);

    @Test
    public void contextLoad() {
        // Nothing to do
    }

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.framework.jpa.multitenant.autoconfigure.sequencegen" })
    @EnableAutoConfiguration
    public static class SequenceGeneratorTestConfiguration {

    }
}
