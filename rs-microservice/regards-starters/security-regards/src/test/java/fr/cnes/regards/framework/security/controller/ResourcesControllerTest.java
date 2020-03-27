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
package fr.cnes.regards.framework.security.controller;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Class ResourcesControllerTest
 *
 * Tests for resources controller
 * @author CS
 */
public class ResourcesControllerTest {

    /**
     * SecurityResourcesController
     */
    private final SecurityResourcesController controller = new SecurityResourcesController(
            new MethodAuthorizationService());

    /**
     * Check the /resources common endpoint that retrieves all resources of a microservice.
     */
    @Requirement("REGARDS_DSL_ADM_ADM_240")
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Check the /resources common endpoint that retrieves all resources of a microservice.")
    @Test
    public void resourcesTest() {

        final ResponseEntity<List<ResourceMapping>> response = controller.getAllResources();
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);

        Assert.assertEquals("There should be 2 resources", 2, response.getBody().size());
        for (ResourceMapping mapping : response.getBody()) {
            if (RequestMethod.GET.equals(mapping.getMethod())) {
                Assert.assertEquals("/tests/endpoint", mapping.getFullPath());
            }
            if (RequestMethod.POST.equals(mapping.getMethod())) {
                Assert.assertEquals("/tests/endpoint/post", mapping.getFullPath());
            }
        }
    }

}
