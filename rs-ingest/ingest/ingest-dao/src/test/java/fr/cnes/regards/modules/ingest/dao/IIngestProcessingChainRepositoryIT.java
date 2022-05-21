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
package fr.cnes.regards.modules.ingest.dao;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalIT;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

/**
 * Test retrieving plugin configuration by chain
 *
 * @author Marc Sordi
 */
@Ignore("Just for testing @Query occasionally")
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema:ingest1" })
public class IIngestProcessingChainRepositoryIT extends AbstractDaoTransactionalIT {

    @Autowired
    private IIngestProcessingChainRepository repo;

    @Test
    public void findValidationPlugin() {
        Optional<PluginConfiguration> conf = repo.findOneValidationPluginByName(IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL);
        Assert.assertTrue(conf.isPresent());
    }
}
