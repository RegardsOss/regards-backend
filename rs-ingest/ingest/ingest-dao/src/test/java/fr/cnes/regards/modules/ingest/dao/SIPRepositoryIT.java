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

import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Test class to verify search with criterion of {@link SIPEntity} entities.
 * @author Sébastien Binda
 */
public class SIPRepositoryIT extends AbstractSIPRepositoryIT {

    @Test
    public void searchSipEntities() {

        Pageable page = PageRequest.of(0, 100);
        List<SIPEntity> res = sipRepository.findAll(SIPEntitySpecifications
                .search(null, null, SESSION_OWNER, SESSION, null, OffsetDateTime.now().minusHours(12),
                        Lists.newArrayList(SIPState.INGESTED), true, null, null, page));
        Assert.assertEquals(4, res.size());

        res = sipRepository.findAll(SIPEntitySpecifications
                .search(null, null, SESSION_OWNER, SESSION, null, OffsetDateTime.now().minusHours(1),
                        Lists.newArrayList(SIPState.INGESTED), true, null, null, page));
        Assert.assertEquals(1, res.size());

        res = sipRepository.findAll(SIPEntitySpecifications.search(null, null, SESSION_OWNER, SESSION, null, null, null, true,
                                                                   null, null, page));
        Assert.assertEquals(4, res.size());

        res = sipRepository
                .findAll(SIPEntitySpecifications.search(null, null, null, SESSION, null, null, null, true, null, null, page));
        Assert.assertEquals(4, res.size());

        res = sipRepository
                .findAll(SIPEntitySpecifications.search(null, null, null, SESSION, null, null,
                                                        Lists.newArrayList(SIPState.INGESTED), true, null, null, page));
        Assert.assertEquals(4, res.size());

        res = sipRepository
                .findAll(SIPEntitySpecifications.search(null, null, null, null, null, null, null, true, null, null, page));
        Assert.assertEquals(4, res.size());

        res = sipRepository.findAll(SIPEntitySpecifications
                .search(null, null, "invalid", "invalid", null, OffsetDateTime.now().minusHours(12),
                        Lists.newArrayList(SIPState.INGESTED), true, null, null, page));
        Assert.assertEquals(0, res.size());

        res = sipRepository.findAll(SIPEntitySpecifications
                .search(null, null, SESSION_OWNER, SESSION, null, OffsetDateTime.now().minusHours(12),
                        Lists.newArrayList(SIPState.INGESTED), true, null, null, page));
        Assert.assertEquals(4, res.size());

        res = sipRepository
                .findAll(SIPEntitySpecifications.search(null, null, SESSION_OWNER, SESSION, null, OffsetDateTime.now(),
                                                        Lists.newArrayList(SIPState.INGESTED), true, null, null, page));
        Assert.assertEquals(0, res.size());

        res = sipRepository
                .findAll(SIPEntitySpecifications.search(null, null, null, null, null, null, null, true, null, null, page));
        Assert.assertEquals(4, res.size());

        // Check order by attribute on ingestDate
        Assert.assertTrue(res.get(0).getCreationDate().compareTo(res.get(1).getCreationDate()) >= 0);
        Assert.assertTrue(res.get(1).getCreationDate().compareTo(res.get(2).getCreationDate()) >= 0);

        res = sipRepository.findAll(SIPEntitySpecifications.search(Sets.newLinkedHashSet("SIP_003"), null, null, null,
                null, null, null, true, null, null, page));
        Assert.assertEquals(2, res.size());

        res = sipRepository.findAll(SIPEntitySpecifications.search(Sets.newLinkedHashSet("SIP_00%"), null, null, null,
                null, null, null, false, null, null, page));
        Assert.assertEquals(0, res.size());

        res = sipRepository.findAll(SIPEntitySpecifications.search(null, Sets.newLinkedHashSet(sip1.getSipId()), null,
                null, null, null, null, true, null, null, page));
        Assert.assertEquals(1, res.size());

        res = sipRepository.findAll(SIPEntitySpecifications.search(null, Sets.newLinkedHashSet(sip1.getSipId()), null,
                null, null, null, null, false, null, null, page));
        Assert.assertEquals(3, res.size());
    }

}
