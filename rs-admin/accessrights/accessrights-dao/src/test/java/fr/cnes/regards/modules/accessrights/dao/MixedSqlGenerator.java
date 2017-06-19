/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 *
 * @author Marc Sordi
 *
 */
//@Ignore("Used to generate SQL script with HBM2DDL, public schema must exist and be empty!")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { MixedSqlGeneratorConfiguration.class })
@TestPropertySource(properties = { "regards.jpa.multitenant.outputFile=target/project_script.sql",
        "regards.jpa.multitenant.migrationTool=HBM2DDL", "regards.jpa.instance.outputFile=target/instance_script.sql",
        "regards.jpa.instance.migrationTool=HBM2DDL" })
//@TestPropertySource(properties = { "regards.jpa.multitenant.migrationTool=FLYWAYDB",
//        "regards.jpa.instance.migrationTool=FLYWAYDB", "spring.jpa.properties.hibernate.default_schema=flyway" })
public class MixedSqlGenerator {

    @Test
    public void generate() {

    }
}
