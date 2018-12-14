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
package fr.cnes.regards.modules.storage.dao;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.InformationPackageProperties;
import fr.cnes.regards.framework.oais.builder.InformationPackagePropertiesBuilder;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=projectdb",
        "spring.application.name=storage", "spring.jpa.properties.hibernate.show_sql=true" })
@ContextConfiguration(classes = DAOTestConfiguration.class)
public class DaoIT extends AbstractDaoTransactionalTest {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(DaoIT.class);

    @Autowired
    private IAIPEntityRepository repo;

    @Autowired
    private IAIPSessionRepository aipSessionRepo;

    @Autowired
    private ICustomizedAIPEntityRepository customAIPEntityRepo;

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

    private static final String SESSION = "SESSION_1";

    @Before
    public void init() throws NoSuchAlgorithmException, MalformedURLException {
        repo.deleteAll();
        aipSessionRepo.deleteAll();
        dao = new AIPDao(repo, customAIPEntityRepo);
        AIPSession aipSession = new AIPSession();
        aipSession.setId(SESSION);
        aipSession.setLastActivationDate(OffsetDateTime.now());
        aipSession = aipSessionRepo.save(aipSession);
        aip1 = generateRandomAIP();
        aip1.setState(AIPState.VALID);
        aip1.getProperties().getPdi().getTags().add("aip");
        aip1.getProperties().getPdi().getTags().add("aip1");
        setSubmissionDate(aip1, OffsetDateTime.now().minusMinutes(10));
        aip1.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(10));
        aip1 = dao.save(aip1, aipSession);
        aip12 = pseudoClone(aip1);
        UniformResourceName version2 = UniformResourceName.fromString(aip12.getId().toString());
        version2.setVersion(2);
        aip12.setId(version2);
        aip12.getProperties().getPdi().getTags().add("aip");
        aip12.getProperties().getPdi().getTags().add("aip1");
        aip12.getProperties().getPdi().getTags().add("aip12");
        aip12 = dao.save(aip12, aipSession);
        aip2 = generateRandomAIP();
        aip2.setState(AIPState.PENDING);
        setSubmissionDate(aip2, OffsetDateTime.now().minusMinutes(20));
        aip2.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(20));
        aip2.getProperties().getPdi().getTags().add("aip");
        aip2 = dao.save(aip2, aipSession);
        aip3 = generateRandomAIP();
        aip3.setState(AIPState.DELETED);
        setSubmissionDate(aip3, OffsetDateTime.now().minusMinutes(30));
        aip3.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(30));
        aip3.getProperties().getPdi().getTags().add("aip");
        aip3 = dao.save(aip3, aipSession);
        aip4 = generateRandomAIP();
        aip4.setState(AIPState.STORAGE_ERROR);
        setSubmissionDate(aip4, OffsetDateTime.now().minusMinutes(40));
        aip4.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(40));
        aip4.getProperties().getPdi().getTags().add("aip");
        aip4 = dao.save(aip4, aipSession);
        aip5 = generateRandomAIP();
        aip5.setState(AIPState.STORED);
        setSubmissionDate(aip5, OffsetDateTime.now().minusMinutes(50));
        aip5.getLastEvent().setDate(OffsetDateTime.now().minusMinutes(50));
        aip5.getProperties().getPdi().getTags().add("aip");
        aip5 = dao.save(aip5, aipSession);
    }

    public AIP pseudoClone(AIP aip) {

        AIPBuilder aipBuilder = new AIPBuilder(aip.getId(), aip.getSipIdUrn(), aip.getProviderId(), aip.getIpType(),
                SESSION);

        AIP clone = aipBuilder.build(aip.getProperties());
        clone.setState(aip.getState());
        return clone;
    }

    public AIP generateRandomAIP() throws NoSuchAlgorithmException, MalformedURLException {

        UniformResourceName sipId = new UniformResourceName(OAISIdentifier.SIP, EntityType.COLLECTION, "tenant",
                UUID.randomUUID(), 1);
        UniformResourceName aipId = new UniformResourceName(OAISIdentifier.AIP, EntityType.COLLECTION, "tenant",
                sipId.getEntityId(), 1);

        String providerId = String.valueOf(generateRandomString(new Random(), 40));

        // Init AIP builder
        AIPBuilder aipBuilder = new AIPBuilder(aipId, Optional.of(sipId), providerId, EntityType.DATA, SESSION);

        return aipBuilder.build(generateRandomInformationPackageProperties(aipId));
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
            ippBuilder.getContentInformationBuilder().setDataObject(DataType.OTHER, "bla", "SHA1", sha1("blahblah"),
                                                                    new Long(new Random().nextInt(10000000)),
                                                                    new URL("ftp://bla"));
            ippBuilder.getContentInformationBuilder().setSyntaxAndSemantic("NAME", "SYNTAX_DESCRIPTION",
                                                                           MimeType.valueOf("application/name"),
                                                                           "DESCRIPTION");
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

        Page<AIP> result = dao.findAllByState(AIPState.DELETED, PageRequest.of(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertTrue(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllByState(AIPState.PENDING, PageRequest.of(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertTrue(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllByState(AIPState.STORAGE_ERROR, PageRequest.of(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertTrue(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));

        result = dao.findAllByState(AIPState.STORED, PageRequest.of(0, 10));
        Assert.assertFalse(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertTrue(result.getContent().contains(aip5));

        result = dao.findAllByState(AIPState.VALID, PageRequest.of(0, 10));
        Assert.assertTrue(result.getContent().contains(aip1));
        Assert.assertFalse(result.getContent().contains(aip2));
        Assert.assertFalse(result.getContent().contains(aip3));
        Assert.assertFalse(result.getContent().contains(aip4));
        Assert.assertFalse(result.getContent().contains(aip5));
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_050")
    @Purpose("System keeps track of all versions of aips")
    public void testFindAllByIpIdStartingWith() {
        String ipIdWithoutVersion = aip1.getId().toString();
        ipIdWithoutVersion = ipIdWithoutVersion.substring(0, ipIdWithoutVersion.indexOf(":V"));
        Page<AIP> aips = dao.findAllByIpIdStartingWith(ipIdWithoutVersion, PageRequest.of(0, 100));
        Assert.assertTrue(aips.getContent().contains(aip1));
        Assert.assertTrue(aips.getContent().contains(aip12));
        Assert.assertFalse(aips.getContent().contains(aip2));
        Assert.assertFalse(aips.getContent().contains(aip3));
        Assert.assertFalse(aips.getContent().contains(aip4));
        Assert.assertFalse(aips.getContent().contains(aip5));
    }

    @Test
    public void testFindAllByTags() {
        // aips have been generated with there own ipId as tag(except for aip12 which is tagged by aip1 ipId), lets
        // retrieve them according to there ipId
        Page<AIP> aips = dao.findAllByTag(aip1.getId().toString(), PageRequest.of(0, 100));
        Assert.assertTrue(aips.getContent().contains(aip1));
        Assert.assertTrue(aips.getContent().contains(aip12));
        Assert.assertFalse(aips.getContent().contains(aip2));
        Assert.assertFalse(aips.getContent().contains(aip3));
        Assert.assertFalse(aips.getContent().contains(aip4));
        Assert.assertFalse(aips.getContent().contains(aip5));

        aips = dao.findAllByTag(aip2.getId().toString(), PageRequest.of(0, 100));
        Assert.assertFalse(aips.getContent().contains(aip1));
        Assert.assertFalse(aips.getContent().contains(aip12));
        Assert.assertTrue(aips.getContent().contains(aip2));
        Assert.assertFalse(aips.getContent().contains(aip3));
        Assert.assertFalse(aips.getContent().contains(aip4));
        Assert.assertFalse(aips.getContent().contains(aip5));

        aips = dao.findAllByTag(aip3.getId().toString(), PageRequest.of(0, 100));
        Assert.assertFalse(aips.getContent().contains(aip1));
        Assert.assertFalse(aips.getContent().contains(aip12));
        Assert.assertFalse(aips.getContent().contains(aip2));
        Assert.assertTrue(aips.getContent().contains(aip3));
        Assert.assertFalse(aips.getContent().contains(aip4));
        Assert.assertFalse(aips.getContent().contains(aip5));

        aips = dao.findAllByTag(aip4.getId().toString(), PageRequest.of(0, 100));
        Assert.assertFalse(aips.getContent().contains(aip1));
        Assert.assertFalse(aips.getContent().contains(aip12));
        Assert.assertFalse(aips.getContent().contains(aip2));
        Assert.assertFalse(aips.getContent().contains(aip3));
        Assert.assertTrue(aips.getContent().contains(aip4));
        Assert.assertFalse(aips.getContent().contains(aip5));

        aips = dao.findAllByTag(aip5.getId().toString(), PageRequest.of(0, 100));
        Assert.assertFalse(aips.getContent().contains(aip1));
        Assert.assertFalse(aips.getContent().contains(aip12));
        Assert.assertFalse(aips.getContent().contains(aip2));
        Assert.assertFalse(aips.getContent().contains(aip3));
        Assert.assertFalse(aips.getContent().contains(aip4));
        Assert.assertTrue(aips.getContent().contains(aip5));
    }

    @Test
    public void testCustomQueryAtLeastOneTag() {
        List<String> tags = Arrays.asList("aip", "aip1", "aip12");

        Page<AIP> aips = dao.findAll(
                                     // test at least one tag
                                     AIPQueryGenerator.searchAIPContainingAtLeastOneTag(null, null, null, tags, null,
                                                                                        null, null, null),
                                     PageRequest.of(0, 100));
        Assert.assertTrue(aips.getContent().contains(aip1));
        Assert.assertTrue(aips.getContent().contains(aip12));
        Assert.assertTrue(aips.getContent().contains(aip2));
        Assert.assertTrue(aips.getContent().contains(aip3));
        Assert.assertTrue(aips.getContent().contains(aip4));
        Assert.assertTrue(aips.getContent().contains(aip5));
    }

    @Test
    public void testCustomQueryAtLeastOneTag2() {
        List<String> tags = Arrays.asList("aip", "aip1", "aip12");

        Page<AIP> aips = dao.findAll(
                                     // test at least one tag
                                     AIPQueryGenerator.searchAIPContainingAtLeastOneTag(AIPState.STORED, null, null,
                                                                                        tags, null, null, null, null),
                                     PageRequest.of(0, 100));
        Assert.assertFalse(aips.getContent().contains(aip1));
        Assert.assertFalse(aips.getContent().contains(aip12));
        Assert.assertFalse(aips.getContent().contains(aip2));
        Assert.assertFalse(aips.getContent().contains(aip3));
        Assert.assertFalse(aips.getContent().contains(aip4));
        Assert.assertTrue(aips.getContent().contains(aip5));
    }

    @Test
    public void testCustomQueryContainingAllTags() {
        List<String> tags = Arrays.asList("aip", "aip1");

        Page<AIP> aips = dao.findAll(
                                     // test at least one tag
                                     AIPQueryGenerator.searchAIPContainingAllTags(AIPState.VALID, null, null, tags,
                                                                                  null, null, null, null),
                                     PageRequest.of(0, 100));
        Assert.assertTrue(aips.getContent().contains(aip1));
        Assert.assertTrue(aips.getContent().contains(aip12));
        Assert.assertFalse(aips.getContent().contains(aip2));
        Assert.assertFalse(aips.getContent().contains(aip3));
        Assert.assertFalse(aips.getContent().contains(aip4));
    }

    @Test
    public void testCustomQueryGetTags() {
        List<String> tags = Arrays.asList("aip");

        List<String> aips = dao.findAllByCustomQuery(
                                                     // test at least one tag
                                                     AIPQueryGenerator.searchAipTagsUsingSQL(AIPState.VALID, null, null,
                                                                                             tags, null, null, null,
                                                                                             null));

        Assert.assertTrue(aips.containsAll(aip1.getTags()));
        Assert.assertTrue(aips.containsAll(aip12.getTags()));
        Assert.assertFalse(aips.containsAll(aip2.getTags()));
        Assert.assertFalse(aips.containsAll(aip3.getTags()));
        Assert.assertFalse(aips.containsAll(aip4.getTags()));
    }
}
