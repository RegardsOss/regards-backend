/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.request.update;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Léo Mieulet
 */
public class AbstractAIPUpdateTaskTypeTest {

    @Test
    public void testOrder() {
        Assert.assertEquals(0, AIPUpdateTaskType.ADD_CATEGORY.getOrder(AIPUpdateTaskType.ADD_TAG));
        Assert.assertEquals(0, AIPUpdateTaskType.REMOVE_FILE_LOCATION.getOrder(AIPUpdateTaskType.REMOVE_FILE_LOCATION));
        Assert.assertEquals(1, AIPUpdateTaskType.ADD_CATEGORY.getOrder(AIPUpdateTaskType.ADD_FILE_LOCATION));
        Assert.assertEquals(-1, AIPUpdateTaskType.ADD_FILE_LOCATION.getOrder(AIPUpdateTaskType.REMOVE_CATEGORY));
    }
}
