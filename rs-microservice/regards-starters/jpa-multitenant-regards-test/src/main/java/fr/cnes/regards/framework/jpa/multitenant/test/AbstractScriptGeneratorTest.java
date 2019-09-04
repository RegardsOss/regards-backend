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
 * Generate SQL script using hbm2ddl tool.<br/>
 * Extends this test class in dao layer, <b>remove ALL SCHEMAS and recreate only public one</b> from the target database
 * and run the
 * test.<br/>
 * A SQL script should be created in target.
 *
 * Maybe you want to initialize database with other FLYWAY modules.<br/>
 * To do that, annotate your class with following code. After initialization, comment these lines to run HBM2DDL engine.
 * <code>
 * &#64;TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=public",
 * "regards.jpa.multitenant.migrationTool=FLYWAYDB" })
 * </code>
 * @author Marc Sordi
 */
@TestPropertySource(properties = { "regards.jpa.multitenant.migrationTool=HBM2DDL",
        "regards.jpa.multitenant.embedded=false", "regards.jpa.multitenant.outputFile=target/project_script.sql",
        "spring.jpa.properties.hibernate.default_schema:sql_generator" })
public abstract class AbstractScriptGeneratorTest extends AbstractDaoTest {

    @Test
    public void generate() {
        // Nothing to do
    }
}
