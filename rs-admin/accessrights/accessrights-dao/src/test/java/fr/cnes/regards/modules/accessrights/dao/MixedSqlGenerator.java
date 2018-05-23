/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.dao;

import org.junit.Ignore;
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
@Ignore("Used to generate SQL script with HBM2DDL, public schema must exist and be empty!")
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
