/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.client;

import java.lang.reflect.ParameterizedType;

import org.elasticsearch.index.IndexNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;

/**
 * Abstract Integration Test for clients of the module.
 *
 * @author Xavier-Alexandre Brochard
 */
@TestPropertySource("classpath:test.properties")
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
public abstract class AbstractSearchClientIT<T> extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSearchClientIT.class);

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    /**
     * ElasticSearch repository
     */
    @Autowired
    private IEsRepository esRepository;

    protected T client;

    @Before
    public void setUp() {
        client = FeignClientBuilder.build(new TokenClientProvider<>(getClazz(),
                "http://" + serverAddress + ":" + getPort(), feignSecurityManager));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Init required index in the ElasticSearch repository
        if (!esRepository.indexExists(getDefaultTenant())) {
            esRepository.createIndex(getDefaultTenant());
        } else {
            esRepository.deleteAll(getDefaultTenant());
        }

        FeignSecurityManager.asSystem();
    }

    @After
    public void tearDown() {
        try {
            esRepository.deleteIndex(getDefaultTenant());
        } catch (IndexNotFoundException e) {
            // Who cares ?
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @SuppressWarnings("unchecked")
    protected Class<T> getClazz() {
        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
