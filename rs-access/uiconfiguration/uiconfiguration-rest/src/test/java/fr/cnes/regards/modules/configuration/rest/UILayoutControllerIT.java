/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.configuration.domain.LayoutDefaultApplicationIds;
import fr.cnes.regards.modules.configuration.domain.UILayout;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Class InstanceLayoutControllerIT
 * <p>
 * IT Tests for REST Controller
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class UILayoutControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(UILayoutControllerIT.class);

    @Test
    public void getUserApplicationLayout() {
        performDefaultGet("/layouts/{applicationId}",
                          customizer().expectStatusOk(),
                          "Plop",
                          LayoutDefaultApplicationIds.USER.toString());
    }

    @Test
    public void updateLayoutWithInvalidJsonFormat() {
        final UILayout UILayout = new UILayout();
        UILayout.setId(1L);
        UILayout.setApplicationId(LayoutDefaultApplicationIds.USER.toString());
        UILayout.setLayout("{}}");
        
        performDefaultPut("/layouts/{applicationId}",
                          UILayout,
                          customizer().expect(status().isUnprocessableEntity()),
                          "Plop",
                          LayoutDefaultApplicationIds.USER.toString());
    }

    @Test
    public void updateLayout() {
        final UILayout UILayout = new UILayout();
        UILayout.setId(1L);
        UILayout.setApplicationId(LayoutDefaultApplicationIds.USER.toString());
        UILayout.setLayout("{\"test\":\"ok\"}");

        performDefaultPut("/layouts/{applicationId}",
                          UILayout,
                          customizer().expectStatusOk(),
                          "Plop",
                          LayoutDefaultApplicationIds.USER.toString());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
