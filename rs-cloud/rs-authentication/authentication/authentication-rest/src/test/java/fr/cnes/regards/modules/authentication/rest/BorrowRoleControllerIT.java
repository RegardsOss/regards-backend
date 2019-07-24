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
package fr.cnes.regards.modules.authentication.rest;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@ContextConfiguration(classes = BorrowRoleITConfiguration.class)
@TestPropertySource(locations = { "classpath:application-test.properties" })
public class BorrowRoleControllerIT extends AbstractRegardsIT {

    @Test
    public void testSwitch() {
        performDefaultGet(BorrowRoleController.PATH_BORROW_ROLE + BorrowRoleController.PATH_BORROW_ROLE_TARGET,
                          customizer().expectStatusOk(), "ERROR", DefaultRole.PUBLIC.toString());
    }

}
