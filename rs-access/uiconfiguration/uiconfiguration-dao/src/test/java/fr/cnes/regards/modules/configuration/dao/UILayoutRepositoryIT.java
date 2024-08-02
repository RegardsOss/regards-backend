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
package fr.cnes.regards.modules.configuration.dao;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalIT;
import fr.cnes.regards.modules.configuration.domain.UILayout;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Class LayoutRepositoryTest
 * <p>
 * DAO Test
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@TestPropertySource("classpath:test.properties")
public class UILayoutRepositoryIT extends AbstractDaoTransactionalIT {

    @Autowired
    private IUILayoutRepository repository;

    @Test
    public void saveLayoutTest() {
        // Create a new layout configuration
        final UILayout UILayout = new UILayout();
        UILayout.setApplicationId("TEST");
        UILayout.setLayout("{}");
        final UILayout newUILayout = repository.save(UILayout);
        final UILayout UILayout2 = repository.findById(newUILayout.getId()).orElse(null);
        Assert.assertEquals(newUILayout.getLayout(), UILayout2.getLayout());
    }

    @Test
    public void updateLayoutTest() {
        // Create a new layout configuration
        final UILayout UILayout = new UILayout();
        UILayout.setApplicationId("TEST");
        UILayout.setLayout("{}");
        final UILayout newUILayout = repository.save(UILayout);
        newUILayout.setLayout("{\"test\":\"test\"}");
        final UILayout UILayout2 = repository.save(newUILayout);
        Assert.assertEquals("{\"test\":\"test\"}", UILayout2.getLayout());
    }

}
