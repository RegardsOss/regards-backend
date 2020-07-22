/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.dao;

import org.junit.ClassRule;
import org.junit.Ignore;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractScriptGeneratorTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;


@Ignore
@TestPropertySource(
    properties = {
        "regards.jpa.multitenant.tenants[0].url=jdbc:tc:postgresql:///rs_testdb_${user.name}",
        "regards.jpa.multitenant.tenants[0].tenant=${user.name}_project"
    }
)
public class ProcessingGeneratorTest extends AbstractScriptGeneratorTest {

    @ClassRule
    public static final PostgreSQLContainer<?> pgsql = new PostgreSQLContainer<>("postgres:9.6.12");

}
