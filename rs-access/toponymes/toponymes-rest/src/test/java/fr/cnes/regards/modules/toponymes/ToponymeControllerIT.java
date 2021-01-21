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
package fr.cnes.regards.modules.toponymes;

import org.junit.Test;

import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;

/**
 *
 * @author Sébastien Binda
 *
 */
public class ToponymeControllerIT extends AbstractRegardsTransactionalIT {

    @Test
    public void findAll() {
        performDefaultGet("/toponymes",
                          customizer().expectStatusOk().addParameter("page", "0").addParameter("size", "1"),
                          "should be  ok");
    }

    @Test
    public void findOne() {
        performDefaultGet(ToponymesController.ROOT_MAPPING + ToponymesController.TOPONYME_ID,
                          customizer().expectStatusOk(), "should be  ok", "France");
    }

}
