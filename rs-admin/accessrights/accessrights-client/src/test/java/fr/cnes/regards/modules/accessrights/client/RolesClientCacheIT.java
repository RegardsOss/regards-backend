package fr.cnes.regards.modules.accessrights.client;
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

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.client.cache.AccessRightsClientCacheAutoConfiguration;
import fr.cnes.regards.modules.accessrights.client.cache.CacheableRolesClient;
import fr.cnes.regards.modules.accessrights.client.cache.IRolesHierarchyKeyGenerator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Sébastien Binda
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { TestConfiguration.class, AccessRightsClientCacheAutoConfiguration.class })
@ActiveProfiles({ "test", "accessrightclientcache" })
public class RolesClientCacheIT {

    @Autowired
    CacheableRolesClient cacheClient;

    @Autowired
    IRolesHierarchyKeyGenerator keyGenerator;

    @Autowired
    MockCounter counter;

    @Test
    public void test() throws EntityNotFoundException {

        cacheClient.shouldAccessToResourceRequiring("plop");
        cacheClient.shouldAccessToResourceRequiring("plop");
        cacheClient.shouldAccessToResourceRequiring("plop");

        Assert.assertEquals(1, counter.getCount());

        cacheClient.shouldAccessToResourceRequiring("plop");

        Assert.assertEquals(1, counter.getCount());

        cacheClient.shouldAccessToResourceRequiring("another");
        cacheClient.shouldAccessToResourceRequiring("another");

        Assert.assertEquals(2, counter.getCount());

        keyGenerator.cleanCache();

        cacheClient.shouldAccessToResourceRequiring("another");
        cacheClient.shouldAccessToResourceRequiring("another");

        Assert.assertEquals(3, counter.getCount());
    }

}
