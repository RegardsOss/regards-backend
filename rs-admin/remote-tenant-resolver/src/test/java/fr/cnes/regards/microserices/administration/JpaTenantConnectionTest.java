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
package fr.cnes.regards.microserices.administration;

import java.io.File;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.microserices.administration.stubs.ProjectClientStub;
import fr.cnes.regards.microservices.administration.RemoteTenantAutoConfiguration;

/**
 *
 * Class JpaTenantConnectionTest
 *
 * Test with jpa multitenant starter database creation.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Ignore("Cannot reach an admin microservice instance in unit test.")
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { JpaTenantConnectionConfiguration.class, RemoteTenantAutoConfiguration.class })
public class JpaTenantConnectionTest {

    /**
     *
     * Check for multitenant resolver throught administration microservice client
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Check for multitenant resolver throught administration microservice client")
    @Test
    public void checkJpaTenants() {

        File resourcesDirectory = new File("target/" + ProjectClientStub.PROJECT_NAME);
        Assert.assertTrue(resourcesDirectory.exists());

        resourcesDirectory = new File("target/test1");
        Assert.assertTrue(resourcesDirectory.exists());

        resourcesDirectory = new File("target/test2");
        Assert.assertTrue(resourcesDirectory.exists());

    }

}
