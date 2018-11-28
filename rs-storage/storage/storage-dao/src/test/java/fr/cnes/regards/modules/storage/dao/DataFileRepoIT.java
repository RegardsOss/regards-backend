package fr.cnes.regards.modules.storage.dao;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.InformationPackageProperties;
import fr.cnes.regards.framework.oais.builder.InformationPackagePropertiesBuilder;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.MonitoringAggregation;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=storage", "spring.application.name=storage" })
@ContextConfiguration(classes = DAOTestConfiguration.class)
public class DataFileRepoIT extends AbstractDaoTransactionalTest {

    private static final String SESSION = "SESSION_1";

    @Autowired
    private IStorageDataFileRepository dataFileRepository;

    @Autowired
    private IAIPEntityRepository aipEntityRepository;

    @Autowired
    private IAIPSessionRepository aipSessionRepo;

    @Autowired
    private ICustomizedAIPEntityRepository customAIPEntityRepo;

    private IAIPDao aipDao;

    private IDataFileDao dataFileDao;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    private Long dataStorage2Id;

    private Long dataStorage1Id;

    private Long dataStorage3Id;

    private Long dataStorage1UsedSize = 0L;

    private Long dataStorage2UsedSize = 0L;

    private Long dataStorage3UsedSize = 0L;

    private AIP aip3;

    @Before
    public void init() throws MalformedURLException, NoSuchAlgorithmException {
        aipDao = new AIPDao(aipEntityRepository, customAIPEntityRepo);
        dataFileDao = new DataFileDao(dataFileRepository, aipEntityRepository);

        AIPSession aipSession = new AIPSession();
        aipSession.setId(SESSION);
        aipSession.setLastActivationDate(OffsetDateTime.now());
        aipSession = aipSessionRepo.save(aipSession);
        // lets get some data storage configurations
        PluginConfiguration dataStorage1 = new PluginConfiguration();
        dataStorage1.setPluginId("LocalDataStorage");
        dataStorage1.setLabel("DataStorage1");
        dataStorage1.setPriorityOrder(0);
        dataStorage1.setVersion("1.0");
        dataStorage1.setInterfaceNames(Sets.newHashSet());
        dataStorage1 = pluginRepo.save(dataStorage1);
        PrioritizedDataStorage pds1 = new PrioritizedDataStorage(dataStorage1, 0L, DataStorageType.ONLINE);
        pds1 = prioritizedDataStorageRepository.save(pds1);
        PluginConfiguration dataStorage2 = new PluginConfiguration();
        dataStorage2.setPluginId("LocalDataStorage");
        dataStorage2.setLabel("DataStorage2");
        dataStorage2.setPriorityOrder(0);
        dataStorage2.setVersion("1.0");
        dataStorage2.setInterfaceNames(Sets.newHashSet());
        dataStorage2 = pluginRepo.save(dataStorage2);
        PrioritizedDataStorage pds2 = new PrioritizedDataStorage(dataStorage2, 1L, DataStorageType.ONLINE);
        pds2 = prioritizedDataStorageRepository.save(pds2);
        PluginConfiguration dataStorage3 = new PluginConfiguration();
        dataStorage3.setPluginId("LocalDataStorage");
        dataStorage3.setLabel("DataStorage3");
        dataStorage3.setPriorityOrder(0);
        dataStorage3.setVersion("1.0");
        dataStorage3.setInterfaceNames(Sets.newHashSet());
        dataStorage3 = pluginRepo.save(dataStorage3);
        PrioritizedDataStorage pds3 = new PrioritizedDataStorage(dataStorage3, 2L, DataStorageType.ONLINE);
        pds3 = prioritizedDataStorageRepository.save(pds3);
        // lets get some aips and dataFiles
        AIP aip1 = generateRandomAIP();
        aip1 = aipDao.save(aip1, aipSession);
        List<StorageDataFile> dataFiles = Lists.newArrayList();
        Set<StorageDataFile> dataFilesAip = StorageDataFile.extractDataFiles(aip1, aipSession);
        for (StorageDataFile df : dataFilesAip) {
            df.addDataStorageUsed(pds1);
            dataStorage1Id = pds1.getId();
            dataStorage1UsedSize += df.getFileSize();
        }
        dataFiles.addAll(dataFilesAip);
        AIP aip2 = generateRandomAIP();
        aip2 = aipDao.save(aip2, aipSession);
        dataFilesAip = StorageDataFile.extractDataFiles(aip2, aipSession);
        for (StorageDataFile df : dataFilesAip) {
            df.addDataStorageUsed(pds2);
            dataStorage2Id = pds2.getId();
            dataStorage2UsedSize += df.getFileSize();
        }
        dataFiles.addAll(dataFilesAip);
        aip3 = generateRandomAIP();
        aip3 = aipDao.save(aip3, aipSession);
        dataFilesAip = StorageDataFile.extractDataFiles(aip3, aipSession);
        for (StorageDataFile df : dataFilesAip) {
            df.addDataStorageUsed(pds3);
            dataStorage3Id = pds3.getId();
            dataStorage3UsedSize += df.getFileSize();
        }
        dataFiles.addAll(dataFilesAip);
        // lets test with a file stored into two archives ( 1 and 2 )
        AIP aip12 = generateRandomAIP();
        aip12 = aipDao.save(aip12, aipSession);
        dataFilesAip = StorageDataFile.extractDataFiles(aip12, aipSession);
        for (StorageDataFile df : dataFilesAip) {
            df.addDataStorageUsed(pds1);
            dataStorage1UsedSize += df.getFileSize();
            df.addDataStorageUsed(pds2);
            dataStorage2UsedSize += df.getFileSize();
        }
        dataFiles.addAll(dataFilesAip);
        dataFileDao.save(dataFiles);
    }

    @Test
    public void testMonitoringAggregation() {
        Collection<MonitoringAggregation> monitoringAggregations = dataFileRepository.getMonitoringAggregation();
        for (MonitoringAggregation agg : monitoringAggregations) {
            if (agg.getDataStorageUsedId().equals(dataStorage1Id)) {
                Assert.assertTrue(agg.getUsedSize().equals(dataStorage1UsedSize));
            }
            if (agg.getDataStorageUsedId().equals(dataStorage2Id)) {
                Assert.assertTrue(agg.getUsedSize().equals(dataStorage2UsedSize));
            }
            if (agg.getDataStorageUsedId().equals(dataStorage3Id)) {
                Assert.assertTrue(agg.getUsedSize().equals(dataStorage3UsedSize));
            }
        }
    }

    @Test
    public void testFindTopByPDS() {
        Set<StorageDataFile> possibleResults = StorageDataFile.extractDataFiles(aip3, aipSessionRepo.findOne(SESSION));
        StorageDataFile result = dataFileRepository.findTopByPrioritizedDataStoragesId(dataStorage3Id);
        Assert.assertNotNull("There should be a data file stored by dataStorage3", result);
        Assert.assertTrue("Result should be one of aip3 data files", possibleResults.contains(result));
    }

    public AIP generateRandomAIP() throws NoSuchAlgorithmException, MalformedURLException {

        UniformResourceName sipId = new UniformResourceName(OAISIdentifier.SIP,
                                                            EntityType.COLLECTION,
                                                            "tenant",
                                                            UUID.randomUUID(),
                                                            1);
        UniformResourceName aipId = new UniformResourceName(OAISIdentifier.AIP,
                                                            EntityType.COLLECTION,
                                                            "tenant",
                                                            sipId.getEntityId(),
                                                            1);

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
            ippBuilder.getContentInformationBuilder().setDataObject(DataType.OTHER,
                                                                    "blah",
                                                                    "SHA1",
                                                                    sha1("blahblah"),
                                                                    new Long((new Random()).nextInt(10000000)),
                                                                    new URL("ftp://bla"));
            ippBuilder.getContentInformationBuilder().setSyntaxAndSemantic("NAME",
                                                                           "SYNTAX_DESCRIPTION",
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
}
