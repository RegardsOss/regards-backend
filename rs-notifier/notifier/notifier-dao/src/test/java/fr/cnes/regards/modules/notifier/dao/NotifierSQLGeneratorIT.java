package fr.cnes.regards.modules.notifier.dao;

import org.junit.Ignore;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractScriptGeneratorIT;

//Use following line to launch FLYWAY on public schema (comment it to use HBM2DDL)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=public",
        "regards.jpa.multitenant.migrationTool=FLYWAYDB" })
@Ignore
public class NotifierSQLGeneratorIT extends AbstractScriptGeneratorIT {
}
