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
package fr.cnes.regards.framework.modules.tinyurl;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.modules.tinyurl.dao.TinyUrlRepository;
import fr.cnes.regards.framework.modules.tinyurl.domain.TinyUrl;
import fr.cnes.regards.framework.modules.tinyurl.service.TinyUrlService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

/**
 * Test {@link TinyUrl} lifecycle
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=tinyurl_purge" })
public class TinyUrlPurgeIT extends AbstractMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(TinyUrlPurgeIT.class);

    @Autowired
    private TinyUrlService tinyUrlService;

    @Autowired
    private TinyUrlRepository tinyUrlRepository;

    @Before
    public void clean() {
        tinyUrlRepository.deleteAllInBatch();
    }

    @Test
    public void purgeTest() {
        // Insert tiny urls
        for (int i = 0; i < 10; i++) {
            tinyUrlService.create("tiny" + i, 0);
        }
        tinyUrlService.create("inOneHour", 1);

        // Check them
        List<TinyUrl> tinyUrlList = tinyUrlRepository.findAll();
        Assert.assertEquals(11, tinyUrlList.size());

        // Purge and check them
        tinyUrlService.purge();
        tinyUrlList = tinyUrlRepository.findAll();
        Assert.assertEquals(1, tinyUrlList.size());
    }
}
