/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.test;

import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Generate SQL script using hbm2ddl tool.<br/>
 * Extends this test class in dao layer, remove and recreate public schema from the target database and run the test.<br/>
 * A SQL script should be created in target.
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "regards.jpa.multitenant.embedded=false",
        "regards.jpa.multitenant.outputFile=target/project_script.sql" })
public class AbstractScriptGeneratorTest extends AbstractDaoTest {

    @Test
    public void generate() {
        // Nothing to do
    }
}
