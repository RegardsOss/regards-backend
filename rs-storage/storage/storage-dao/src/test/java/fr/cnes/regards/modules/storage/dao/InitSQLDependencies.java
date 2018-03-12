package fr.cnes.regards.modules.storage.dao;

import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;

/**
 * Initialize database with Flyway scripts on public schema.
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=public" })
public class InitSQLDependencies extends AbstractDaoTest {

    @Test
    public void initFlywayDependencies() {
        // Nothing to do
    }
}