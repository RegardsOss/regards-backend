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
package fr.cnes.regards.modules.configuration.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.configuration.domain.Layout;
import fr.cnes.regards.modules.configuration.domain.LayoutDefaultApplicationIds;

/**
 *
 * Class InstanceLayoutControllerIT
 *
 * IT Tests for REST Controller
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class LayoutControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(LayoutControllerIT.class);

    @Test
    public void getUserApplicationLayout() {
        performDefaultGet("/layouts/{applicationId}", customizer().expectStatusOk(), "Plop",
                          LayoutDefaultApplicationIds.USER.toString());
    }

    @Test
    public void updateLayoutWithInvalidJsonFormat() {
        final Layout layout = new Layout();
        layout.setId(1L);
        layout.setApplicationId(LayoutDefaultApplicationIds.USER.toString());
        layout.setLayout("{}}");
        performDefaultPut("/layouts/{applicationId}", layout, customizer().expect(status().isUnprocessableEntity()),
                          "Plop", LayoutDefaultApplicationIds.USER.toString());
    }

    @Test
    public void updateLayout() {
        final Layout layout = new Layout();
        layout.setId(1L);
        layout.setApplicationId(LayoutDefaultApplicationIds.USER.toString());
        layout.setLayout("{\"test\":\"ok\"}");
        performDefaultPut("/layouts/{applicationId}", layout, customizer().expectStatusOk(), "Plop",
                          LayoutDefaultApplicationIds.USER.toString());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
