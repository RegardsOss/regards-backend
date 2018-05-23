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
package fr.cnes.regards.modules.authentication.rest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@ContextConfiguration(classes = BorrowRoleITConfiguration.class)
@TestPropertySource(locations = { "classpath:application-test.properties" })
public class BorrowRoleControllerIT extends AbstractRegardsIT {

    private static final Logger LOG = LoggerFactory.getLogger(BorrowRoleControllerIT.class);

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Test
    public void testSwitch() {
        List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(BorrowRoleController.PATH_BORROW_ROLE + BorrowRoleController.PATH_BORROW_ROLE_TARGET,
                          expectations, "ERROR", DefaultRole.PUBLIC.toString());
    }

}
