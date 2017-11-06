package fr.cnes.regards.modules.ingest.dao;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * Test class to verify search with criterion of {@link SIPEntity} entities.
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema:ingest_dao" })
public class SIPSearchRepositoryTest extends AbstractDaoTransactionalTest {

    @Autowired
    private ISIPRepository sipRepository;

    @Before
    public void init() {

        SIPEntity sip = new SIPEntity();
        SIPBuilder b = new SIPBuilder("SIP_001");
        sip.setSip(b.build());
        sip.setIpId("URN:SIP:Collection:DEFAULT:1234567890:1");
        sip.setSipId("SIP_001");
        sip.setIngestDate(OffsetDateTime.now());
        sip.setOwner("admin");
        sip.setProcessing("processing");
        sip.setSessionId("sessionId");
        sip.setState(SIPState.CREATED);
        sip.setVersion(1);
        sip.setChecksum("1234567890");

        sipRepository.save(sip);

        sip = new SIPEntity();
        b = new SIPBuilder("SIP_002");
        sip.setSip(b.build());
        sip.setIpId("URN:SIP:Collection:DEFAULT:1234567890:2");
        sip.setSipId("SIP_002");
        sip.setIngestDate(OffsetDateTime.now().minusHours(6));
        sip.setOwner("admin");
        sip.setProcessing("processing");
        sip.setSessionId("sessionId");
        sip.setState(SIPState.CREATED);
        sip.setVersion(2);
        sip.setChecksum("12345678902");

        sipRepository.save(sip);

        sip = new SIPEntity();
        b = new SIPBuilder("SIP_003");
        sip.setSip(b.build());
        sip.setIpId("URN:SIP:Collection:DEFAULT:1234567890:3");
        sip.setSipId("SIP_003");
        sip.setIngestDate(OffsetDateTime.now().minusHours(6));
        sip.setOwner("admin2");
        sip.setProcessing("processing2");
        sip.setSessionId("sessionId2");
        sip.setState(SIPState.STORED);
        sip.setVersion(3);
        sip.setChecksum("12345678903");

        sipRepository.save(sip);
    }

    @Test
    public void searchSipEntities() {

        List<SIPEntity> res = sipRepository.findAll(SIPEntitySpecifications
                .search("sessionId", "admin", OffsetDateTime.now().minusHours(12), SIPState.CREATED));
        Assert.assertTrue(res.size() == 2);

        res = sipRepository.findAll(SIPEntitySpecifications
                .search("sessionId", "admin", OffsetDateTime.now().minusHours(1), SIPState.CREATED));
        Assert.assertTrue(res.size() == 1);

        res = sipRepository.findAll(SIPEntitySpecifications.search("sessionId", null, null, null));
        Assert.assertTrue(res.size() == 2);

        res = sipRepository.findAll(SIPEntitySpecifications.search(null, "admin", null, null));
        Assert.assertTrue(res.size() == 2);

        res = sipRepository.findAll(SIPEntitySpecifications.search(null, null, null, SIPState.CREATED));
        Assert.assertTrue(res.size() == 2);

        res = sipRepository.findAll(SIPEntitySpecifications
                .search("invalid", "admin", OffsetDateTime.now().minusHours(12), SIPState.CREATED));
        Assert.assertTrue(res.size() == 0);

        res = sipRepository.findAll(SIPEntitySpecifications
                .search("sessionId", "unvalid", OffsetDateTime.now().minusHours(12), SIPState.CREATED));
        Assert.assertTrue(res.size() == 0);

        res = sipRepository
                .findAll(SIPEntitySpecifications.search("sessionId", "admin", OffsetDateTime.now(), SIPState.CREATED));
        Assert.assertTrue(res.size() == 0);

        res = sipRepository.findAll(SIPEntitySpecifications
                .search("sessionId", "admin", OffsetDateTime.now().minusHours(12), SIPState.AIP_CREATED));
        Assert.assertTrue(res.size() == 0);

        res = sipRepository.findAll(SIPEntitySpecifications.search(null, null, null, null));
        Assert.assertTrue(res.size() == 3);

        // Check order by attribute on ingestDate
        Assert.assertTrue(res.get(0).getIngestDate().compareTo(res.get(1).getIngestDate()) > 0);
        Assert.assertTrue(res.get(1).getIngestDate().compareTo(res.get(2).getIngestDate()) > 0);

    }

}
