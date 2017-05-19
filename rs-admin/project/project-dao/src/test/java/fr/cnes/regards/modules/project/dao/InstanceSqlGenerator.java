/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao;

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
@ContextConfiguration(classes = { InstanceSqlGeneratorConfiguration.class })
@TestPropertySource(properties = { "regards.jpa.multitenant.migrationTool=HBM2DDL",
        "regards.jpa.instance.outputFile=target/instance_script.sql", "regards.jpa.instance.migrationTool=HBM2DDL" })
public class InstanceSqlGenerator {

    @Test
    public void generate() {

    }
}
