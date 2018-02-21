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
package fr.cnes.regards.modules.ingest.dao;

import java.time.OffsetDateTime;
import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;

import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * Test class to verify search with criterion of {@link SIPEntity} entities.
 * @author Sébastien Binda
 */
public class SIPRepositoryTest extends AbstractSIPRepositoryTest {

    @Test
    public void searchSipEntities() {

        List<SIPEntity> res = sipRepository
                .findAll(SIPEntitySpecifications.search(null, "sessionId", "admin", OffsetDateTime.now().minusHours(12),
                                                        Lists.newArrayList(SIPState.CREATED), PROCESSING_CHAIN));
        Assert.assertTrue(res.size() == 2);

        res = sipRepository
                .findAll(SIPEntitySpecifications.search(null, "sessionId", "admin", OffsetDateTime.now().minusHours(1),
                                                        Lists.newArrayList(SIPState.CREATED), PROCESSING_CHAIN));
        Assert.assertTrue(res.size() == 1);

        res = sipRepository.findAll(SIPEntitySpecifications.search(null, "sessionId", null, null, null, null));
        Assert.assertTrue(res.size() == 2);

        res = sipRepository.findAll(SIPEntitySpecifications.search(null, null, "admin", null, null, null));
        Assert.assertTrue(res.size() == 2);

        res = sipRepository.findAll(SIPEntitySpecifications.search(null, null, null, null,
                                                                   Lists.newArrayList(SIPState.CREATED), null));
        Assert.assertTrue(res.size() == 2);

        res = sipRepository.findAll(SIPEntitySpecifications.search(null, null, null, null, null, PROCESSING_CHAIN));
        Assert.assertTrue(res.size() == 3);

        res = sipRepository
                .findAll(SIPEntitySpecifications.search(null, "invalid", "admin", OffsetDateTime.now().minusHours(12),
                                                        Lists.newArrayList(SIPState.CREATED), null));
        Assert.assertTrue(res.size() == 0);

        res = sipRepository.findAll(SIPEntitySpecifications.search(null, "sessionId", "unvalid",
                                                                   OffsetDateTime.now().minusHours(12),
                                                                   Lists.newArrayList(SIPState.CREATED), null));
        Assert.assertTrue(res.size() == 0);

        res = sipRepository.findAll(SIPEntitySpecifications.search(null, "sessionId", "admin", OffsetDateTime.now(),
                                                                   Lists.newArrayList(SIPState.CREATED), null));
        Assert.assertTrue(res.size() == 0);

        res = sipRepository
                .findAll(SIPEntitySpecifications.search(null, "sessionId", "admin", OffsetDateTime.now().minusHours(12),
                                                        Lists.newArrayList(SIPState.AIP_CREATED), null));
        Assert.assertTrue(res.size() == 0);

        res = sipRepository.findAll(SIPEntitySpecifications.search(null, null, null, null, null, null));
        Assert.assertTrue(res.size() == 4);

        // Check order by attribute on ingestDate
        Assert.assertTrue(res.get(0).getIngestDate().compareTo(res.get(1).getIngestDate()) >= 0);
        Assert.assertTrue(res.get(1).getIngestDate().compareTo(res.get(2).getIngestDate()) >= 0);

        res = sipRepository.findAll(SIPEntitySpecifications.search("SIP_003", null, null, null, null, null));
        Assert.assertTrue(res.size() == 2);

    }

}
