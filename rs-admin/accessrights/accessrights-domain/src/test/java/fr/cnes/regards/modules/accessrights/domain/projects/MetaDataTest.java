/**
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.domain.projects;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.modules.accessrights.domain.UserVisibility;

/**
 * Unit testing of {@link MetaData}
 *
 * @author Maxime Bouveron
 */
public class MetaDataTest {

    /**
     * Test MetaData
     */
    private MetaData metaData;

    /**
     * Test id
     */
    private final Long id = 0L;

    /**
     * Test key
     */
    private final String key = "key";

    /**
     * Test value
     */
    private final String value = "val";

    /**
     * Test UserVisibility value
     */
    private final UserVisibility visibility = UserVisibility.READABLE;

    @Before
    public void setUp() {
        metaData = new MetaData();
        metaData.setId(id);
        metaData.setKey(key);
        metaData.setValue(value);
        metaData.setVisibility(visibility);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.MetaData#MetaData()}.
     */
    @Test
    public void testMetaData() {
        final MetaData meta = new MetaData();
        Assert.assertNull(meta.getId());
        Assert.assertNull(meta.getKey());
        Assert.assertNull(meta.getValue());
        Assert.assertNull(meta.getVisibility());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.MetaData#getId()}.
     */
    @Test
    public void testGetId() {
        Assert.assertEquals(id, metaData.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.MetaData#setId(java.lang.Long)}.
     */
    @Test
    public void testSetId() {
        final Long newId = 4L;
        metaData.setId(newId);
        Assert.assertEquals(newId, metaData.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.MetaData#getKey()}.
     */
    @Test
    public void testGetKey() {
        Assert.assertEquals(key, metaData.getKey());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.MetaData#setKey(java.lang.String)}.
     */
    @Test
    public void testSetKey() {
        final String newKey = "newKey";
        metaData.setKey(newKey);
        Assert.assertEquals(newKey, metaData.getKey());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.MetaData#getValue()}.
     */
    @Test
    public void testGetValue() {
        Assert.assertEquals(value, metaData.getValue());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.MetaData#setValue(java.lang.String)}.
     */
    @Test
    public void testSetValue() {
        final String newValue = "newValue";
        metaData.setValue(newValue);
        Assert.assertEquals(newValue, metaData.getValue());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.MetaData#getVisibility()}.
     */
    @Test
    public void testGetVisibility() {
        Assert.assertEquals(visibility, metaData.getVisibility());
    }

    /**
     * Test method for {@link MetaData#setVisibility(UserVisibility)}.
     */
    @Test
    public void testSetVisibility() {
        metaData.setVisibility(UserVisibility.HIDDEN);
        Assert.assertEquals(UserVisibility.HIDDEN, metaData.getVisibility());
    }

}
