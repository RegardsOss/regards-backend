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

import java.util.Optional;

/**
 * Test {@link fr.cnes.regards.framework.modules.tinyurl.domain.TinyUrl} lifecycle
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=tinyurl" })
public class TinyUrlServiceIT extends AbstractMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(TinyUrlServiceIT.class);

    @Autowired
    private TinyUrlService tinyUrlService;

    @Autowired
    private TinyUrlRepository tinyUrlRepository;

    @Before
    public void clean() {
        tinyUrlRepository.deleteAllInBatch();
    }

    @Test
    public void load() {
        // Nothing to do
    }

    @Test
    public void createTest00() {
        // Create
        TinyContext context = new TinyContext().setHello("Hello context!");
        TinyUrl tinyUrl = tinyUrlService.create(context);

        // Retrieve
        Optional<TinyUrl> tinyUrl1 = tinyUrlService.get(tinyUrl.getUuid());
        if (!tinyUrl1.isPresent()) {
            Assert.fail("Tiny URL must exist!");
        }
        TinyContext context2 = tinyUrlService.loadContext(tinyUrl1.get(), TinyContext.class);
        Assert.assertEquals(context.getHello(), context2.getHello());
    }

    public static class TinyContext {

        String hello;

        public String getHello() {
            return hello;
        }

        public TinyContext setHello(String hello) {
            this.hello = hello;
            return this;
        }
    }
}
