/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.jpa.multitenant.test;

import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Generate the diff between already existing SQL migration scripts and content of your model.<br/>
 * Extends this test class in dao layer and ensure the <b>public schema is empty</b> on the target database.
 * Then you can safely run the test : <br/>
 * 1] To do that, annotate your class with following code.
 * <code>
 * @TestPropertySource(properties = {"regards.jpa.multitenant.migrationTool=FLYWAYDB"})
 * </code>
 * Then execute your test. It should execute FLYWAY migration (all SQL files in your module) against the public schema.
 * 2] Comment the annotation you added above your class and execute another time the migration (this time using HBM2DDL).
 * You can now find in the target folder the SQL diff that you can use to complete your FLYWAY migration.
 *
 * @author Marc Sordi
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "regards.jpa.multitenant.migrationTool=HBM2DDL",
        "regards.jpa.multitenant.embedded=false", "regards.jpa.multitenant.outputFile=target/project_script.sql",
        "spring.jpa.properties.hibernate.default_schema=public" })
public abstract class AbstractScriptGeneratorTest extends AbstractDaoTest {

    @Test
    public void generate() {
        // Nothing to do
    }
}
