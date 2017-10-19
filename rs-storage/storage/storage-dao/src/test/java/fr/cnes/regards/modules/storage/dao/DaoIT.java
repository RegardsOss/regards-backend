/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.dao;

import javax.persistence.UniqueConstraint;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.InformationPackageProperties;
import fr.cnes.regards.framework.oais.builder.InformationPackagePropertiesBuilder;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=projectdb", "spring.application.name=storage" })
public class DaoIT extends AbstractDaoTransactionalTest {

    private static final Logger LOG = LoggerFactory.getLogger(DaoIT.class);

    @Autowired
    private IAIPDataBaseRepository repo;

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
        repo.deleteAll();
        dao = new AIPDao(repo);
        aip1 = generateRandomAIP();
        aip1.setState(AIPState.VALID);
        setSubmissionDate(aip1, OffsetDateTime.now().minusMinutes(10));
        aip1.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(10));
        aip1 = dao.save(aip1);
        aip12 = pseudoClone(aip1);
        UniformResourceName version2 = UniformResourceName.fromString(aip12.getId().toString());
        version2.setVersion(2);
        aip12.setId(version2);
        aip12 = dao.save(aip12);
        aip2 = generateRandomAIP();
        aip2.setState(AIPState.PENDING);
        setSubmissionDate(aip2, OffsetDateTime.now().minusMinutes(20));
        aip2.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(20));
        aip2 = dao.save(aip2);
        aip3 = generateRandomAIP();
        aip3.setState(AIPState.DELETED);
        setSubmissionDate(aip3, OffsetDateTime.now().minusMinutes(30));
        aip3.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(30));
        aip3 = dao.save(aip3);
        aip4 = generateRandomAIP();
        aip4.setState(AIPState.STORAGE_ERROR);
        setSubmissionDate(aip4, OffsetDateTime.now().minusMinutes(40));
        aip4.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(40));
        aip4 = dao.save(aip4);
        aip5 = generateRandomAIP();
        aip5.setState(AIPState.STORED);
        setSubmissionDate(aip5, OffsetDateTime.now().minusMinutes(50));
        aip5.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(50));
        aip5 = dao.save(aip5);
    }

    public AIP pseudoClone(AIP src) {

        AIPBuilder aipBuilder = new AIPBuilder(src.getId(), src.getSipId(), src.getIpType());

        AIP clone = aipBuilder.build(src.getProperties());
        clone.setState(src.getState());
        return clone;
    }

    public AIP generateRandomAIP() throws NoSuchAlgorithmException, MalformedURLException {

        UniformResourceName ipId = new UniformResourceName(OAISIdentifier.AIP, EntityType.COLLECTION, "tenant",
                UUID.randomUUID(), 1);
        String sipId = String.valueOf(generateRandomString(new Random(), 40));

        // Init AIP builder
        AIPBuilder aipBuilder = new AIPBuilder(ipId, sipId, EntityType.DATA);

        return aipBuilder.build(generateRandomInformationPackageProperties(ipId));
    }

    public InformationPackageProperties generateRandomInformationPackageProperties(UniformResourceName ipId)
            throws NoSuchAlgorithmException, MalformedURLException {

        // Init Information object builder
        InformationPackagePropertiesBuilder ippBuilder = new InformationPackagePropertiesBuilder();
        // Content information
        generateRandomContentInformations(ippBuilder);
        // PDI
        ippBuilder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(),
                                                                 "addition of this aip into our beautiful system!",
                                                                 OffsetDateTime.now());
        // - ContextInformation
        ippBuilder.getPDIBuilder().addTags(generateRandomTags(ipId));
        // - Provenance
        ippBuilder.getPDIBuilder().setFacility("TestPerf");
        // - Access right
        Random random = new Random();
        int maxStringLength = 20;
        ippBuilder.getPDIBuilder().setAccessRightInformation(generateRandomString(random, maxStringLength));

        return ippBuilder.build();
    }

    private void generateRandomContentInformations(InformationPackagePropertiesBuilder ippBuilder)
            throws NoSuchAlgorithmException, MalformedURLException {
        int listMaxSize = 5;
        Random random = new Random();
        int listSize = random.nextInt(listMaxSize) + 1;
        for (int i = 0; i < listSize; i++) {
            ippBuilder.getContentInformationBuilder().setDataObject(DataType.OTHER, new URL("ftp://bla"), null, "SHA1",
                                                                    sha1("blahblah"),
                                                                    new Long((new Random()).nextInt(10000000)));
            ippBuilder.getContentInformationBuilder().setSyntaxAndSemantic("NAME", "SYNTAX_DESCRIPTION",
                                                                           "application/name", "DESCRIPTION");
            ippBuilder.addContentInformation();
        }
    }

    private String generateRandomString(Random random, int maxStringLength) {
        String possibleLetters = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWYXZ";
        int stringSize = random.nextInt(maxStringLength) + 1;
        char[] string = new char[stringSize];
        for (int j = 0; j < stringSize; j++) {
            string[j] = possibleLetters.charAt(random.nextInt(possibleLetters.length()));
        }
        return new String(string);
    }

    private String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        byte[] result = digest.digest(input.getBytes());
        StringBuffer sb = new StringBuffer();
        for (byte element : result) {
            sb.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    /**
     * generate random tags (length and content) but always have at least one tag which is the aip IP ID
     * @param ipId
     * @return
     */
    private String[] generateRandomTags(UniformResourceName ipId) {
        int listMaxSize = 15;
        int tagMaxSize = 10;
        Random random = new Random();
        int listSize = random.nextInt(listMaxSize) + 1;
        String[] tags = new String[listSize];
        tags[0] = ipId.toString();
        for (int i = 1; i < listSize; i++) {
            tags[i] = generateRandomString(random, tagMaxSize);
        }
        return tags;
    }

    private void setSubmissionDate(AIP aip1, OffsetDateTime submissionDate) {
        Event submissionEvent = aip1.getSubmissionEvent();
        submissionEvent.setComment("Submission of this aip into our beautiful system");
        submissionEvent.setDate(submissionDate);
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
        Page<AIP> result = dao.findAllByLastEventDateBefore(aip1.getLastEvent().getDate().plusSeconds(1),
                                                            new PageRequest(0, 10));
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
        Page<AIP> result = dao.findAllByStateAndLastEventDateBefore(aip1.getState(),
                                                                    aip1.getLastEvent().getDate().plusSeconds(1),
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
        Page<AIP> result = dao.findAllBySubmissionDateAfterAndLastEventDateBefore(aip1.getSubmissionEvent().getDate()
                .minusNanos(1), aip1.getLastEvent().getDate().plusSeconds(1), new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllBySubmissionDateAfterAndLastEventDateBefore(aip2.getSubmissionEvent().getDate()
                .minusNanos(1), aip1.getLastEvent().getDate().plusSeconds(1), new PageRequest(0, 10));
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
        Page<AIP> result = dao.findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(aip1.getState(), aip1
                .getSubmissionEvent().getDate().minusNanos(1), aip1.getLastEvent().getDate().plusSeconds(1),
                                                                                          new PageRequest(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(aip2.getState(), aip1
                .getSubmissionEvent().getDate().minusNanos(1), aip1.getLastEvent().getDate().plusSeconds(1),
                                                                                new PageRequest(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));
    }

    @Test
    public void testFindAllByIpIdStartingWith() {
        String ipIdWithoutVersion = aip1.getId().toString();
        ipIdWithoutVersion = ipIdWithoutVersion.substring(0, ipIdWithoutVersion.indexOf(":V"));
        Set<AIP> aips = dao.findAllByIpIdStartingWith(ipIdWithoutVersion);
        Assert.assertTrue(aips.contains(aip1));
        Assert.assertTrue(aips.contains(aip12));
        Assert.assertFalse(aips.contains(aip2));
        Assert.assertFalse(aips.contains(aip3));
        Assert.assertFalse(aips.contains(aip4));
        Assert.assertFalse(aips.contains(aip5));
    }

    @Test
    public void testFindAllByTags() {
        //aips have been generated with there own ipId as tag(except for aip12 which is tagged by aip1 ipId), lets retrieve them according to there ipId
        Set<AIP> aips = dao.findAllByTags(aip1.getId().toString());
        Assert.assertTrue(aips.contains(aip1));
        Assert.assertTrue(aips.contains(aip12));
        Assert.assertFalse(aips.contains(aip2));
        Assert.assertFalse(aips.contains(aip3));
        Assert.assertFalse(aips.contains(aip4));
        Assert.assertFalse(aips.contains(aip5));

        aips = dao.findAllByTags(aip2.getId().toString());
        Assert.assertFalse(aips.contains(aip1));
        Assert.assertFalse(aips.contains(aip12));
        Assert.assertTrue(aips.contains(aip2));
        Assert.assertFalse(aips.contains(aip3));
        Assert.assertFalse(aips.contains(aip4));
        Assert.assertFalse(aips.contains(aip5));

        aips = dao.findAllByTags(aip3.getId().toString());
        Assert.assertFalse(aips.contains(aip1));
        Assert.assertFalse(aips.contains(aip12));
        Assert.assertFalse(aips.contains(aip2));
        Assert.assertTrue(aips.contains(aip3));
        Assert.assertFalse(aips.contains(aip4));
        Assert.assertFalse(aips.contains(aip5));

        aips = dao.findAllByTags(aip4.getId().toString());
        Assert.assertFalse(aips.contains(aip1));
        Assert.assertFalse(aips.contains(aip12));
        Assert.assertFalse(aips.contains(aip2));
        Assert.assertFalse(aips.contains(aip3));
        Assert.assertTrue(aips.contains(aip4));
        Assert.assertFalse(aips.contains(aip5));

        aips = dao.findAllByTags(aip5.getId().toString());
        Assert.assertFalse(aips.contains(aip1));
        Assert.assertFalse(aips.contains(aip12));
        Assert.assertFalse(aips.contains(aip2));
        Assert.assertFalse(aips.contains(aip3));
        Assert.assertFalse(aips.contains(aip4));
        Assert.assertTrue(aips.contains(aip5));
    }

}
