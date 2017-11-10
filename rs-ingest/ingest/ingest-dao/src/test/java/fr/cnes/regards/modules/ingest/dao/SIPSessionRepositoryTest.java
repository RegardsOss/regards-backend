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

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;

public class SIPSessionRepositoryTest extends AbstractSIPRepositoryTest {

    @Test
    public void searchSipSessions() {

        List<SIPSession> sessions = sipSessionRepository.findAll(SIPSessionSpecifications.search(null, null, null));
        Assert.assertEquals(3, sessions.size());

        sessions = sipSessionRepository.findAll(SIPSessionSpecifications.search("sessionId", null, null));
        Assert.assertEquals(2, sessions.size());

        sessions = sipSessionRepository.findAll(SIPSessionSpecifications
                .search(null, OffsetDateTime.now().minusHours(1), OffsetDateTime.now()));
        Assert.assertEquals(3, sessions.size());

        sessions = sipSessionRepository.findAll(SIPSessionSpecifications
                .search("otherSession", OffsetDateTime.now().minusHours(1), OffsetDateTime.now()));
        Assert.assertEquals(1, sessions.size());

    }

}
