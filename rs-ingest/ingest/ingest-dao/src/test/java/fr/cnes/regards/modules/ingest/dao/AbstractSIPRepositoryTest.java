/*
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
package fr.cnes.regards.modules.ingest.dao;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.transaction.BeforeTransaction;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema:ingest_dao" })
public abstract class AbstractSIPRepositoryTest extends AbstractDaoTest {

    private static final Set<String> CATEGORIES = Sets.newHashSet("CATEGORY");

    @BeforeTransaction
    public void beforeTransaction() {
        injectDefaultToken();
    }

    @Autowired
    protected ISIPRepository sipRepository;

    protected static final String PROCESSING_CHAIN = "processing";

    protected static final String PROCESSING_CHAIN2 = "processing2";

    protected static final String SESSION_OWNER = "SESSION_OWNER";

    protected static final String SESSION = "session";

    protected SIPEntity sip1;

    @Before
    public void init() {
        sip1 = new SIPEntity();

        sip1.setSip(SIP.build(EntityType.DATA, "SIP_001"));
        sip1.setSipId(OaisUniformResourceName
                .fromString("URN:SIP:COLLECTION:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        sip1.setProviderId("SIP_001");
        sip1.setCreationDate(OffsetDateTime.now());
        sip1.setLastUpdate(OffsetDateTime.now());
        sip1.setSessionOwner(SESSION_OWNER);
        sip1.setSession(SESSION);
        sip1.setCategories(CATEGORIES);
        sip1.setState(SIPState.INGESTED);
        sip1.setVersion(1);
        sip1.setChecksum("1234567890");

        sip1 = sipRepository.save(sip1);

        SIPEntity sip2 = new SIPEntity();
        sip2.setSip(SIP.build(EntityType.DATA, "SIP_002"));
        sip2.setSipId(OaisUniformResourceName
                .fromString("URN:SIP:COLLECTION:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        sip2.setProviderId("SIP_002");
        sip2.setCreationDate(OffsetDateTime.now().minusHours(6));
        sip2.setLastUpdate(OffsetDateTime.now().minusHours(6));
        sip2.setSessionOwner(SESSION_OWNER);
        sip2.setSession(SESSION);
        sip2.setCategories(CATEGORIES);
        sip2.setState(SIPState.INGESTED);
        sip2.setVersion(1);
        sip2.setChecksum("12345678902");

        sip2 = sipRepository.save(sip2);

        SIPEntity sip3 = new SIPEntity();
        sip3.setSip(SIP.build(EntityType.DATA, "SIP_003"));
        sip3.setSipId(OaisUniformResourceName
                .fromString("URN:SIP:COLLECTION:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        sip3.setProviderId("SIP_003");
        sip3.setCreationDate(OffsetDateTime.now().minusHours(6));
        sip3.setLastUpdate(OffsetDateTime.now().minusHours(6));
        sip3.setSessionOwner(SESSION_OWNER);
        sip3.setSession(SESSION);
        sip3.setCategories(CATEGORIES);
        sip3.setState(SIPState.INGESTED);
        sip3.setVersion(1);
        sip3.setChecksum("12345678903");

        sip3 = sipRepository.save(sip3);

        SIPEntity sip4 = new SIPEntity();

        sip4.setSip(SIP.build(EntityType.DATA, "SIP_001").withDescriptiveInformation("version", "2"));
        sip4.setSipId(OaisUniformResourceName
                .fromString("URN:SIP:COLLECTION:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        sip4.setProviderId("SIP_003");
        sip4.setCreationDate(OffsetDateTime.now().minusHours(6));
        sip4.setLastUpdate(OffsetDateTime.now().minusHours(6));
        sip4.setSessionOwner(SESSION_OWNER);
        sip4.setSession(SESSION);
        sip4.setCategories(CATEGORIES);
        sip4.setState(SIPState.INGESTED);
        sip4.setVersion(2);
        sip4.setChecksum("123456789032");

        sip4 = sipRepository.save(sip4);
    }

    @After
    public void cleanUp() {
        sipRepository.deleteAll();
    }
}
