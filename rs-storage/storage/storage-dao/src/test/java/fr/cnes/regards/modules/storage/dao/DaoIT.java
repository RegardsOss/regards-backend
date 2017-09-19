/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.dao;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.oais.InformationObject;
import fr.cnes.regards.framework.oais.builder.InformationObjectBuilder;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.EventType;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TestPropertySource("classpath:dao-storage.properties")
public class DaoIT extends AbstractDaoTransactionalTest {

    @Autowired
    private IAIPDataBaseRepository repo;

    // @Autowired
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
        aip1 = generateRandomAIP();
        aip1.setState(AIPState.VALID);
        setSubmissionDate(aip1, OffsetDateTime.now().minusMinutes(10));
        aip1.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(10));
        aip1 = dao.save(aip1);
        aip12 = pseudoClone(aip1);
        UniformResourceName version2 = UniformResourceName.fromString(aip12.getIpId());
        version2.setVersion(2);
        aip12.setIpId(version2.toString());
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

        AIPBuilder aipBuilder = new AIPBuilder(src.getType(), src.getIpId(), src.getSipId());
        aipBuilder.addTags(Lists.newArrayList(src.getTags()));
        aipBuilder.addEvents(src.getHistory());
        aipBuilder.addInformationObjects(Lists.newArrayList(src.getInformationObjects()));

        AIP clone = aipBuilder.build();
        clone.setState(src.getState());
        return clone;
    }

    public AIP generateRandomAIP() throws NoSuchAlgorithmException, MalformedURLException {

        String ipId = new UniformResourceName(OAISIdentifier.AIP, EntityType.COLLECTION, "tenant", UUID.randomUUID(), 1)
                .toString();
        String sipId = String.valueOf(generateRandomString(new Random(), 40));

        // Init AIP builder
        AIPBuilder aipBuilder = new AIPBuilder(EntityType.COLLECTION, ipId, sipId);
        aipBuilder.addTags(generateRandomTags());
        aipBuilder.addEvent(EventType.SUBMISSION.name(), "addition of this aip into our beautiful system!",
                            OffsetDateTime.now());
        aipBuilder.addInformationObjects(generateRandomInformationObjects());

        return aipBuilder.build();
    }

    private List<InformationObject> generateRandomInformationObjects()
            throws NoSuchAlgorithmException, MalformedURLException {

        int listMaxSize = 5;
        Random random = new Random();
        int listSize = random.nextInt(listMaxSize) + 1;
        List<InformationObject> informationObjects = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            informationObjects.add(generateRandomInformationObject());
        }
        return informationObjects;
    }

    public InformationObject generateRandomInformationObject() throws NoSuchAlgorithmException, MalformedURLException {

        // Init Information object builder
        InformationObjectBuilder ioBuilder = new InformationObjectBuilder();

        // Content information
        ioBuilder.getContentInformationBuilder().setDataObject(DataType.OTHER, new URL("ftp://bla"));
        ioBuilder.getContentInformationBuilder().setSyntaxAndSemantic("NAME", "SYNTAX_DESCRIPTION", "application/name",
                                                                      "DESCRIPTION");

        // PDI
        // - Provenance
        ioBuilder.getPDIBuilder().setProvenanceInformation("TestPerf");
        // - Fixity
        ioBuilder.getPDIBuilder().setFixityInformation(sha1("blahblah"), "SHA1",
                                                       new Long((new Random()).nextInt(10000000)));
        // - Access right
        Random random = new Random();
        int maxStringLength = 20;
        ioBuilder.getPDIBuilder().setAccessRightInformation(generateRandomString(random, maxStringLength),
                                                            generateRandomString(random, maxStringLength),
                                                            generateRandomString(random, maxStringLength));

        return ioBuilder.build();
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

    private String[] generateRandomTags() {
        int listMaxSize = 15;
        int tagMaxSize = 10;
        Random random = new Random();
        int listSize = random.nextInt(listMaxSize) + 1;
        String[] tags = new String[listSize];
        for (int i = 0; i < listSize; i++) {
            tags[i] = generateRandomString(random, tagMaxSize);
        }
        return tags;
    }

    private void setSubmissionDate(AIP aip1, OffsetDateTime submissionDate) {
        aip1.addEvent(EventType.SUBMISSION.name(), "Submission of this aip into our beautiful system", submissionDate);
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

    public AIP pseudoClone(AIP src) {
        AIP result = new AIP(src.getType());
        result.setInformationObjects(Lists.newArrayList(src.getInformationObjects()));
        result.setIpId(String.valueOf(src.getIpId()));
        result.setSipId(String.valueOf(src.getSipId()));
        result.setTags(Lists.newArrayList(src.getTags()));
        result.setHistory(Lists.newArrayList(src.getHistory()));
        result.setState(AIPState.valueOf(src.getState().toString()));
        return result;
    }

}
