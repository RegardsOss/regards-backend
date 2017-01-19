/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.dao;

import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AipType;
import fr.cnes.regards.modules.storage.urn.UniformResourceName;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TestPropertySource("classpath:dao-storage.properties")
public class DAOTest extends AbstractDaoTransactionalTest {

    @Autowired
    private IAIPRepository repository;

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
        aip1 = new AIP(AipType.COLLECTION).generateAIP();
        aip1.setState(AIPState.VALID);
        aip1.setSubmissionDate(LocalDateTime.now().minusMinutes(10));
        aip1.getLastEvent().setDate(LocalDateTime.now().minusMinutes(10));
        aip12 = aip1;
        aip1 = repository.save(aip1);
        UniformResourceName version2 = UniformResourceName.fromString(aip12.getIpId());
        version2.setVersion(2);
        aip12.setIpId(version2.toString());
        aip12 = repository.save(aip12);
        aip2 = new AIP(AipType.COLLECTION).generateAIP();
        aip2.setState(AIPState.PENDING);
        aip2.setSubmissionDate(LocalDateTime.now().minusMinutes(20));
        aip2.getLastEvent().setDate(LocalDateTime.now().minusMinutes(20));
        aip2 = repository.save(aip2);
        aip3 = new AIP(AipType.COLLECTION).generateAIP();
        aip3.setState(AIPState.DELETED);
        aip3.setSubmissionDate(LocalDateTime.now().minusMinutes(30));
        aip3.getLastEvent().setDate(LocalDateTime.now().minusMinutes(30));
        aip3 = repository.save(aip3);
        aip4 = new AIP(AipType.COLLECTION).generateAIP();
        aip4.setState(AIPState.STORAGE_ERROR);
        aip4.setSubmissionDate(LocalDateTime.now().minusMinutes(40));
        aip4.getLastEvent().setDate(LocalDateTime.now().minusMinutes(40));
        aip4 = repository.save(aip4);
        aip5 = new AIP(AipType.COLLECTION).generateAIP();
        aip5.setState(AIPState.STORED);
        aip5.setSubmissionDate(LocalDateTime.now().minusMinutes(50));
        aip5.getLastEvent().setDate(LocalDateTime.now().minusMinutes(50));
        aip5 = repository.save(aip5);
    }

    @Test
    public void testFindByState() {
        Page<AIP> result = repository.findAllByState(AIPState.DELETED, new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = repository.findAllByState(AIPState.PENDING, new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = repository.findAllByState(AIPState.STORAGE_ERROR, new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = repository.findAllByState(AIPState.STORED, new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));

        result = repository.findAllByState(AIPState.VALID, new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));
    }

    @Test
    public void testFindBySubmissionDateAfter() {
        Page<AIP> result = repository.findAllBySubmissionDateAfter(aip5.getSubmissionDate().minusNanos(1),
                                                                   new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));

        result = repository.findAllBySubmissionDateAfter(aip4.getSubmissionDate().minusNanos(1),
                                                         new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = repository.findAllBySubmissionDateAfter(aip3.getSubmissionDate().minusNanos(1),
                                                         new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = repository.findAllBySubmissionDateAfter(aip2.getSubmissionDate().minusNanos(1),
                                                         new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = repository.findAllBySubmissionDateAfter(aip1.getSubmissionDate().minusNanos(1),
                                                         new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));
    }

    @Test
    public void testFindByLastEventDateBefore() {
        Page<AIP> result = repository.findAllByLastEventDateBefore(aip1.getLastEvent().getDate().plusNanos(1),
                                                                   new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));

        result = repository.findAllByLastEventDateBefore(aip2.getLastEvent().getDate().plusNanos(1),
                                                         new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));

        result = repository.findAllByLastEventDateBefore(aip3.getLastEvent().getDate().plusNanos(1),
                                                         new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));

        result = repository.findAllByLastEventDateBefore(aip4.getLastEvent().getDate().plusNanos(1),
                                                         new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));

        result = repository.findAllByLastEventDateBefore(aip5.getLastEvent().getDate().plusNanos(1),
                                                         new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));
    }

    @Test
    public void testFindAllByStateAndLastEventDateBefore() {
        Page<AIP> result = repository.findAllByStateAndLastEventDateBefore(aip1.getState(),
                                                                           aip1.getLastEvent().getDate().plusNanos(1),
                                                                           new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = repository.findAllByStateAndLastEventDateBefore(aip2.getState(),
                                                                 aip1.getLastEvent().getDate().plusNanos(1),
                                                                 new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

    }

    @Test
    public void testFindAllBySubmissionDateAfterAndLastEventDateBefore() {
        Page<AIP> result = repository.findAllBySubmissionDateAfterAndLastEventDateBefore(aip1.getSubmissionDate()
                .minusNanos(1), aip1.getLastEvent().getDate().plusNanos(1), new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = repository.findAllBySubmissionDateAfterAndLastEventDateBefore(aip2.getSubmissionDate().minusNanos(1),
                                                                               aip1.getLastEvent().getDate()
                                                                                       .plusNanos(1),
                                                                               new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));
    }

    @Test
    public void testFindAllByStateAndSubmissionDateAfter() {
        Page<AIP> result = repository.findAllByStateAndSubmissionDateAfter(aip1.getState(),
                                                                           aip1.getSubmissionDate().minusNanos(1),
                                                                           new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = repository.findAllByStateAndSubmissionDateAfter(aip2.getState(),
                                                                 aip1.getSubmissionDate().minusNanos(1),
                                                                 new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));
    }

    @Test
    public void testFindAllByStateAndSubmissionDateAfterAndLastEventDateBefore() {
        Page<AIP> result = repository.findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(aip1.getState(), aip1
                .getSubmissionDate().minusNanos(1), aip1.getLastEvent().getDate().plusNanos(1), new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = repository.findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(aip2.getState(), aip1
                .getSubmissionDate().minusNanos(1), aip1.getLastEvent().getDate().plusNanos(1), new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));
    }

    @Test
    public void testFindOneByIpIdWithDataObjects() {
        AIP requested = repository.findOneByIpIdWithDataObjects(aip1.getIpId());
        Assert.assertEquals(aip1.getDataObjects().size(), requested.getDataObjects().size());
    }

    @Test
    public void testFindAllByIpIdStartingWith() {
        String ipIdWithoutVersion = aip1.getIpId();
        ipIdWithoutVersion = ipIdWithoutVersion.substring(0, ipIdWithoutVersion.indexOf(":V"));
        List<AIP> aips = repository.findAllByIpIdStartingWith(ipIdWithoutVersion);
        Assert.assertTrue(aips.contains(aip1));
        Assert.assertTrue(aips.contains(aip12));
        Assert.assertFalse(aips.contains(aip2));
        Assert.assertFalse(aips.contains(aip3));
        Assert.assertFalse(aips.contains(aip4));
        Assert.assertFalse(aips.contains(aip5));
    }
}
