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
package fr.cnes.regards.modules.ingest.dao;

import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SipAIPState;

/**
 * @author Marc Sordi
 *
 */
@Ignore("Remove this test class")
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema:aip_submission" })
public class AIPRepoTest extends AbstractDaoTransactionalTest {

    @Autowired
    private IAIPRepository aipRepo;

    @Test
    public void test2() {
        Set<AIPEntity> aips = aipRepo.findBySipProcessingAndState("DefaultProcessingChain", SipAIPState.CREATED);
        Assert.assertNotNull(aips);
    }
}
