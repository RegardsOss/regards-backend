/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.configuration.dao;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.modules.configuration.domain.Layout;

/**
 *
 * Class LayoutRepositoryTest
 *
 * DAO Test
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@TestPropertySource("classpath:test.properties")
public class LayoutRepositoryTest extends AbstractDaoTransactionalTest {

    @Autowired
    private ILayoutRepository repository;

    @Test
    public void saveLayoutTest() {
        // Create a new layout configuration
        final Layout layout = new Layout();
        layout.setApplicationId("TEST");
        layout.setLayout("{}");
        final Layout newLayout = repository.save(layout);
        final Layout layout2 = repository.findOne(newLayout.getId());
        Assert.assertEquals(newLayout.getLayout(), layout2.getLayout());
    }

    @Test
    public void updateLayoutTest() {
        // Create a new layout configuration
        final Layout layout = new Layout();
        layout.setApplicationId("TEST");
        layout.setLayout("{}");
        final Layout newLayout = repository.save(layout);
        newLayout.setLayout("{\"test\":\"test\"}");
        final Layout layout2 = repository.save(newLayout);
        Assert.assertEquals("{\"test\":\"test\"}", layout2.getLayout());
    }

}
