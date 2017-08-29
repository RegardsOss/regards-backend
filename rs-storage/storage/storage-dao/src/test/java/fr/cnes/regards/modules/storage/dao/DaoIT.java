/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.dao;

import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.Event;
import fr.cnes.regards.modules.storage.domain.EventType;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TestPropertySource("classpath:dao-storage.properties")
public class DaoIT extends AbstractDaoTransactionalTest {

    @Autowired
    private IAIPDataBaseRepository repo;

    //    @Autowired
    private IAIPDao dao;

    /**
     * last submitted and last updated
     */
    private AIP aip1;

    /**
     * version 2 of aip1
     */
    private AIP aip12;

    /**
     * submitted before aip1 and updated before aip1
     */
    private AIP aip2;

    /**
     * submitted before aip2 and updated before aip2
     */
    private AIP aip3;

    /**
     * submitted before aip3 and updated before aip3
     */
    private AIP aip4;

    /**
     * submitted before aip4 and updated before aip4 ie first submitted and first updated
     */
    private AIP aip5;

    @Before
    public void init() throws NoSuchAlgorithmException, MalformedURLException {
        dao = new AIPDao(repo);
        aip1 = new AIP(EntityType.COLLECTION).generateRandomAIP();
        aip1.setState(AIPState.VALID);
        setSubmissionDate(aip1, OffsetDateTime.now().minusMinutes(10));
        aip1.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(10));
        aip1 = dao.save(aip1);
        aip12 = new AIP(aip1);
        UniformResourceName version2 = UniformResourceName.fromString(aip12.getIpId());
        version2.setVersion(2);
        aip12.setIpId(version2.toString());
        aip12 = dao.save(aip12);
        aip2 = new AIP(EntityType.COLLECTION).generateRandomAIP();
        aip2.setState(AIPState.PENDING);
        setSubmissionDate(aip2, OffsetDateTime.now().minusMinutes(20));
        aip2.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(20));
        aip2 = dao.save(aip2);
        aip3 = new AIP(EntityType.COLLECTION).generateRandomAIP();
        aip3.setState(AIPState.DELETED);
        setSubmissionDate(aip3, OffsetDateTime.now().minusMinutes(30));
        aip3.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(30));
        aip3 = dao.save(aip3);
        aip4 = new AIP(EntityType.COLLECTION).generateRandomAIP();
        aip4.setState(AIPState.STORAGE_ERROR);
        setSubmissionDate(aip4, OffsetDateTime.now().minusMinutes(40));
        aip4.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(40));
        aip4 = dao.save(aip4);
        aip5 = new AIP(EntityType.COLLECTION).generateRandomAIP();
        aip5.setState(AIPState.STORED);
        setSubmissionDate(aip5, OffsetDateTime.now().minusMinutes(50));
        aip5.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(50));
        aip5 = dao.save(aip5);
    }

    private void setSubmissionDate(AIP aip1, OffsetDateTime submissionDate) {
        aip1.getHistory().add(new Event("Submission of this aip into our beautiful system", submissionDate,
                                        EventType.SUBMISSION));
    }

    @Test
    public void testFindByState() {
        Page<AIP> result = dao.findAllByState(AIPState.DELETED, new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllByState(AIPState.PENDING, new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllByState(AIPState.STORAGE_ERROR, new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllByState(AIPState.STORED, new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));

        result = dao.findAllByState(AIPState.VALID, new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));
    }

    @Test
    public void testFindBySubmissionDateAfter() {
        Page<AIP> result = dao.findAllBySubmissionDateAfter(aip5.getSubmissionEvent().getDate().minusNanos(1),
                                                            new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));

        result = dao.findAllBySubmissionDateAfter(aip4.getSubmissionEvent().getDate().minusNanos(1),
                                                  new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllBySubmissionDateAfter(aip3.getSubmissionEvent().getDate().minusNanos(1),
                                                  new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllBySubmissionDateAfter(aip2.getSubmissionEvent().getDate().minusNanos(1),
                                                  new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllBySubmissionDateAfter(aip1.getSubmissionEvent().getDate().minusNanos(1),
                                                  new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));
    }

    @Test
    public void testFindByLastEventDateBefore() {
        Page<AIP> result = dao
                .findAllByLastEventDateBefore(aip1.getLastEvent().getDate().plusSeconds(1), new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));

        result = dao.findAllByLastEventDateBefore(aip2.getLastEvent().getDate().plusSeconds(1), new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));

        result = dao.findAllByLastEventDateBefore(aip3.getLastEvent().getDate().plusSeconds(1), new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));

        result = dao.findAllByLastEventDateBefore(aip4.getLastEvent().getDate().plusSeconds(1), new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));

        result = dao.findAllByLastEventDateBefore(aip5.getLastEvent().getDate().plusSeconds(1), new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));
    }

    @Test
    public void testFindAllByStateAndLastEventDateBefore() {
        Page<AIP> result = dao
                .findAllByStateAndLastEventDateBefore(aip1.getState(), aip1.getLastEvent().getDate().plusSeconds(1),
                                                      new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllByStateAndLastEventDateBefore(aip2.getState(), aip1.getLastEvent().getDate().plusSeconds(1),
                                                          new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

    }

    @Test
    public void testFindAllBySubmissionDateAfterAndLastEventDateBefore() {
        Page<AIP> result = dao
                .findAllBySubmissionDateAfterAndLastEventDateBefore(aip1.getSubmissionEvent().getDate().minusNanos(1),
                                                                    aip1.getLastEvent().getDate().plusSeconds(1),
                                                                    new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao
                .findAllBySubmissionDateAfterAndLastEventDateBefore(aip2.getSubmissionEvent().getDate().minusNanos(1),
                                                                    aip1.getLastEvent().getDate().plusSeconds(1),
                                                                    new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));
    }

    @Test
    public void testFindAllByStateAndSubmissionDateAfter() {
        Page<AIP> result = dao.findAllByStateAndSubmissionDateAfter(aip1.getState(),
                                                                    aip1.getSubmissionEvent().getDate().minusNanos(1),
                                                                    new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllByStateAndSubmissionDateAfter(aip2.getState(),
                                                          aip1.getSubmissionEvent().getDate().minusNanos(1),
                                                          new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));
    }

    @Test
    public void testFindAllByStateAndSubmissionDateAfterAndLastEventDateBefore() {
        Page<AIP> result = dao.findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(aip1.getState(),
                                                                                          aip1.getSubmissionEvent()
                                                                                                  .getDate()
                                                                                                  .minusNanos(1),
                                                                                          aip1.getLastEvent().getDate()
                                                                                                  .plusSeconds(1),
                                                                                          new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(aip2.getState(),
                                                                                aip1.getSubmissionEvent().getDate()
                                                                                        .minusNanos(1),
                                                                                aip1.getLastEvent().getDate()
                                                                                        .plusSeconds(1),
                                                                                new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));
    }

    //FIXME: to remove?
    //    @Test
    //    public void testFindOneByIpIdWithDataObjects() {
    //        AIP requested = dao.findOneByIpIdWithDataObjects(aip1.getIpId());
    //        Assert.assertEquals(aip1.getDataObjects().size(), requested.getDataObjects().size());
    //    }

    @Test
    public void testFindAllByIpIdStartingWith() {
        String ipIdWithoutVersion = aip1.getIpId();
        ipIdWithoutVersion = ipIdWithoutVersion.substring(0, ipIdWithoutVersion.indexOf(":V"));
        Set<AIP> aips = dao.findAllByIpIdStartingWith(ipIdWithoutVersion);
        Assert.assertTrue(aips.contains(aip1));
        Assert.assertTrue(aips.contains(aip12));
        Assert.assertFalse(aips.contains(aip2));
        Assert.assertFalse(aips.contains(aip3));
        Assert.assertFalse(aips.contains(aip4));
        Assert.assertFalse(aips.contains(aip5));
    }
}
