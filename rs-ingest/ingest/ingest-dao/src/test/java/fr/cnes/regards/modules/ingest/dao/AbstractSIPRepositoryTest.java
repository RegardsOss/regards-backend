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

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.oais.builder.InformationPackagePropertiesBuilder;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPSessionBuilder;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema:ingest_dao" })
public abstract class AbstractSIPRepositoryTest extends AbstractDaoTransactionalTest {

    @Autowired
    protected ISIPRepository sipRepository;

    @Autowired
    protected ISIPSessionRepository sipSessionRepository;

    protected static final String PROCESSING_CHAIN = "processing";

    protected static final String PROCESSING_CHAIN2 = "processing2";

    @Before
    public void init() {

        SIPSession session = sipSessionRepository.save(SIPSessionBuilder.build("sessionId"));
        SIPSession session2 = sipSessionRepository.save(SIPSessionBuilder.build("sessionId2"));
        sipSessionRepository.save(SIPSessionBuilder.build("otherSession"));

        SIPEntity sip1 = new SIPEntity();
        SIPBuilder b = new SIPBuilder("SIP_001");
        sip1.setSip(b.build());
        sip1.setSipId(UniformResourceName
                .fromString("URN:SIP:COLLECTION:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        sip1.setProviderId("SIP_001");
        sip1.setIngestDate(OffsetDateTime.now());
        sip1.setOwner("admin");
        sip1.setProcessing(PROCESSING_CHAIN);
        sip1.setSession(session);
        sip1.setState(SIPState.CREATED);
        sip1.setVersion(1);
        sip1.setChecksum("1234567890");

        sip1 = sipRepository.save(sip1);

        SIPEntity sip2 = new SIPEntity();
        b = new SIPBuilder("SIP_002");
        sip2.setSip(b.build());
        sip2.setSipId(UniformResourceName
                .fromString("URN:SIP:COLLECTION:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        sip2.setProviderId("SIP_002");
        sip2.setIngestDate(OffsetDateTime.now().minusHours(6));
        sip2.setOwner("admin");
        sip2.setProcessing(PROCESSING_CHAIN);
        sip2.setSession(session);
        sip2.setState(SIPState.CREATED);
        sip2.setVersion(1);
        sip2.setChecksum("12345678902");

        sip2 = sipRepository.save(sip2);

        SIPEntity sip3 = new SIPEntity();
        b = new SIPBuilder("SIP_003");
        sip3.setSip(b.build());
        sip3.setSipId(UniformResourceName
                .fromString("URN:SIP:COLLECTION:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        sip3.setProviderId("SIP_003");
        sip3.setIngestDate(OffsetDateTime.now().minusHours(6));
        sip3.setOwner("admin2");
        sip3.setProcessing(PROCESSING_CHAIN);
        sip3.setSession(session2);
        sip3.setState(SIPState.STORED);
        sip3.setVersion(1);
        sip3.setChecksum("12345678903");

        sip3 = sipRepository.save(sip3);

        SIPEntity sip4 = new SIPEntity();
        InformationPackagePropertiesBuilder ippb = new InformationPackagePropertiesBuilder();
        ippb.addDescriptiveInformation("version", "2");
        b = new SIPBuilder("SIP_003");
        sip4.setSip(b.build(ippb.build()));
        sip4.setSipId(UniformResourceName
                .fromString("URN:SIP:COLLECTION:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        sip4.setProviderId("SIP_003");
        sip4.setIngestDate(OffsetDateTime.now().minusHours(6));
        sip4.setOwner("admin2");
        sip4.setProcessing(PROCESSING_CHAIN2);
        sip4.setSession(session2);
        sip4.setState(SIPState.STORED);
        sip4.setVersion(2);
        sip4.setChecksum("123456789032");

        sip4 = sipRepository.save(sip4);
    }
}
