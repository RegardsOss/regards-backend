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
package fr.cnes.regards.framework.security.endpoint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Class DefaultAuthorityProviderTest
 *
 * Test class for IAuthoritiesProvider default implemetation
 * @author sbinda
 * @since 1.0-SNAPSHT
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class DefaultAuthorityProviderTest {

    /**
     * Default auhtorities provider defined in configuration class
     */
    @Autowired
    private IAuthoritiesProvider provider;

    /**
     * defaultAuthorityProviderTest
     * @throws SecurityException when no role with passed name could be found
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify access to all resources access per microservice")
    @Test
    public void defaultAuthorityProviderTest() {

        // TODO
    }

}
