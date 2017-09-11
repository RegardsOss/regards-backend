/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugins.staf;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.staf.STAFArchive;
import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;
import fr.cnes.regards.framework.staf.STAFConfiguration;
import fr.cnes.regards.framework.staf.STAFException;
import fr.cnes.regards.framework.staf.STAFManager;
import fr.cnes.regards.framework.staf.STAFService;
import fr.cnes.regards.framework.staf.STAFSession;
import fr.cnes.regards.modules.storage.plugin.staf.domain.AbstractPhysicalFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.PhysicalFileStatusEnum;
import fr.cnes.regards.modules.storage.plugin.staf.domain.STAFController;
import fr.cnes.regards.modules.storage.plugin.staf.domain.TARController;
import fr.cnes.regards.modules.storage.plugin.staf.domain.protocol.STAFURLStreamHandlerFactory;
import fr.cnes.regards.modules.storage.plugin.staf.domain.protocol.STAFUrlFactory;
import fr.cnes.regards.modules.storage.plugin.staf.domain.protocol.STAFUrlParameter;

public class STAFControllerTest {

    private final Set<Path> filesToArchive = Sets.newHashSet();

    private final Set<Path> filesToArchiveWithoutInvalides = Sets.newHashSet();

    private STAFArchive stafArchive;

    private STAFConfiguration configuration;

    private STAFService stafService;

    private STAFController controller;

    private STAFSession stafSessionMock;

    private final static String STAF_ARCHIVE_NAME = "ARCHIVE_TEST";

    private final static String STAF_ARCHIVE_PASSWORD = "password";

    private final static String STAF_TEST_NODE = "/test/node";

    private final static Path STAF_WORKSPACE_PATH = Paths.get(new File("target/STAF/workspace").getAbsolutePath());

    @BeforeClass
    public static void initAll() throws IOException {

        // TODO Add in STAF starter !!!
        try {
            URL.setURLStreamHandlerFactory(new STAFURLStreamHandlerFactory());
        } catch (Error e) {
            // Nothing to do.
        }

        if (Files.exists(STAF_WORKSPACE_PATH)) {
            Files.setPosixFilePermissions(STAF_WORKSPACE_PATH,
                                          Sets.newHashSet(PosixFilePermission.OWNER_EXECUTE,
                                                          PosixFilePermission.OWNER_READ,
                                                          PosixFilePermission.OWNER_WRITE));
            FileUtils.deleteDirectory(STAF_WORKSPACE_PATH.toFile());
        }
    }

    /**
     * After each test, delete workspace directory.
     * @throws IOException
     */
    @After
    public void postTest() throws IOException {
        if (Files.exists(STAF_WORKSPACE_PATH)) {
            Files.setPosixFilePermissions(STAF_WORKSPACE_PATH,
                                          Sets.newHashSet(PosixFilePermission.OWNER_EXECUTE,
                                                          PosixFilePermission.OWNER_READ,
                                                          PosixFilePermission.OWNER_WRITE));
            FileUtils.deleteDirectory(STAF_WORKSPACE_PATH.toFile());
        }
    }

    /**
     * Initialize STAF Configuration and STAF Manager before each test
     * @throws STAFException
     * @throws IOException
     */
    @Before
    public void init() throws STAFException, IOException {

        filesToArchiveWithoutInvalides.add(Paths.get("src/test/resources/staf/income/file_test_1.txt"));
        filesToArchiveWithoutInvalides.add(Paths.get("src/test/resources/staf/income/file_test_2.txt"));
        filesToArchiveWithoutInvalides.add(Paths.get("src/test/resources/staf/income/file_test_3.txt"));
        filesToArchiveWithoutInvalides.add(Paths.get("src/test/resources/staf/income/file_test_4.txt"));
        filesToArchiveWithoutInvalides.add(Paths.get("src/test/resources/staf/income/file_test_5.txt"));

        filesToArchive.addAll(filesToArchiveWithoutInvalides);
        filesToArchive.add(Paths.get("src/test/resources/staf/income/invalid_test_file.txt"));

        configuration = new STAFConfiguration();
        configuration.setMinFileSize(5000L);
        configuration.setMaxFileSize(1000L);

        configuration.setTarSizeThreshold(4500L);
        configuration.setMaxTarSize(5000L);
        configuration.setMaxTarArchivingHours(50L);

        configuration.setAttemptsBeforeFail(5);
        configuration.setBiggerFileGenClass("CS1");
        configuration.setBiggerFileGFClass("CS2");
        configuration.setMaxSessionsArchivingMode(10);
        configuration.setMaxSessionsRestitutionMode(10);
        configuration.setMaxSessionStreamsArchivingMode(10);
        configuration.setMaxSessionStreamsRestitutionMode(10);
        configuration.setMaxStreamFilesArchivingMode(10);
        configuration.setMaxStreamFilesRestitutionMode(10);

        stafArchive = new STAFArchive();
        stafArchive.setArchiveName(STAF_ARCHIVE_NAME);
        stafArchive.setPassword(STAF_ARCHIVE_PASSWORD);

        stafSessionMock = Mockito.mock(STAFSession.class);
        STAFManager stafManagerMock = Mockito.mock(STAFManager.class);

        Mockito.when(stafManagerMock.getConfiguration()).thenReturn(configuration);
        Mockito.when(stafManagerMock.getReservation(Mockito.any())).thenReturn(0);
        // Simulate archive is ok for all files.
        Mockito.when(stafSessionMock.staffilArchive(Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
                .thenAnswer(pInvocation -> {
                    Map<String, String> files = pInvocation.getArgumentAt(0, Map.class);
                    return Lists.newArrayList(files.keySet());
                });
        List<Integer> sessions = Lists.newArrayList();
        sessions.add(0);
        Mockito.when(stafManagerMock.getReservations()).thenReturn(sessions);
        Mockito.when(stafManagerMock.getNewSession()).thenReturn(stafSessionMock);

        stafService = new STAFService(stafManagerMock, stafArchive);

        controller = new STAFController(configuration, STAF_WORKSPACE_PATH, stafService);

    }

    /**
     * Testing the STAFController to check that files are well stored in NORMAL Mode {@link STAFArchiveModeEnum}<br/>
     * <ul>
     * <li>6 Files are to store</li>
     * <li>5 Files are accessible</li>
     * <li>1 File is not accessible</li>
     * <ul>
     *
     * @throws IOException
     * @throws STAFException
     * @throws URISyntaxException
     */
    @Test
    public void testStoreInNormalMode() throws IOException, STAFException, URISyntaxException {

        Map<String, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(STAF_TEST_NODE, filesToArchive);

        controller.prepareFilesToArchive(filesToArchivePerNode, STAFArchiveModeEnum.NORMAL);
        Assert.assertEquals(5, controller.getAllPreparedFilesToArchive().size());

        // All files are ready for transfer
        Set<AbstractPhysicalFile> tarToSendToStaf = controller.getAllPreparedFilesToArchive().stream()
                .filter(pf -> PhysicalFileStatusEnum.TO_STORE.equals(pf.getStatus())).collect(Collectors.toSet());
        Assert.assertEquals(5, tarToSendToStaf.size());

        // Construct map of files to archive to test staffFileArchive method call
        Map<String, String> localFileToArchiveMap = Maps.newHashMap();
        localFileToArchiveMap.put("src/test/resources/staf/income/file_test_3.txt",
                                  "/test/node/1f4add9aecfc4c623cdda55771f4b984");
        localFileToArchiveMap.put("src/test/resources/staf/income/file_test_4.txt",
                                  "/test/node/955fd5652aadd97329a50e029163f3a9");
        localFileToArchiveMap.put("src/test/resources/staf/income/file_test_5.txt",
                                  "/test/node/61142380c96f899eaea71b229dcc4247");
        localFileToArchiveMap.put("src/test/resources/staf/income/file_test_2.txt",
                                  "/test/node/8e3d5e32119c70881316a1a2b17a64d1");
        localFileToArchiveMap.put("src/test/resources/staf/income/file_test_1.txt",
                                  "/test/node/eadcc622739d58e8a78170b67c6ff9f5");

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        Set<AbstractPhysicalFile> archivedFiles = controller.doArchivePreparedFiles(false);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilArchive(localFileToArchiveMap, "CS1", false);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        Assert.assertEquals(5, archivedFiles.size());

        // Check that STAF Controller return all raw files has archived.
        Map<Path, URL> rawArchivedFiles = controller.getRawFilesArchived();
        rawArchivedFiles.forEach((p, u) -> System.out.println(String.format("%s --> %s", p.toString(), u.toString())));
        Assert.assertEquals(5, rawArchivedFiles.size());
        filesToArchiveWithoutInvalides
                .forEach(file -> Assert.assertTrue("Missing a stored file in STAFController raw files stored",
                                                   rawArchivedFiles.keySet().contains(file)));

        // Check that there is 5 file url for the 5 files stored.
        Assert.assertEquals("There should be 5 files URL from STAF for the 5 raw files to archive", 5,
                            rawArchivedFiles.values().stream().distinct().collect(Collectors.toSet()).size());

    }

    /**
     * Testing the STAFController to check that files are well stored in TAR Mode {@link STAFArchiveModeEnum}
     * <ul>
     * <li>6 Files are to store</li>
     * <li>5 Files are accessible</li>
     * <li>1 File is not accessible</li>
     * <li>No current TAR existing</li>
     * <li>Max tar size = 5000octets</li>
     * <li>Min tar size = 4500octets</li>
     * <li>TAR archiving hours limit = 50 hours</li>
     * </ul>
     * Results expected :
     * <ul>
     * <li>3 Tars prepared</li>
     * <li>2 Tars sent to STAF</li>
     * <li>1 Tar stored locally.</li>
     * <ul>
     *
     * @throws IOException
     * @throws STAFException
     * @throws URISyntaxException
     */
    @Test
    public void testStoreInTARMode() throws IOException, STAFException {

        // TAR limits configuration
        configuration.setTarSizeThreshold(4500L);
        configuration.setMaxTarSize(5000L);
        configuration.setMaxTarArchivingHours(50L);

        // Initialize test files
        Map<String, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(STAF_TEST_NODE, filesToArchive);

        // Run STAF file preparation
        controller.prepareFilesToArchive(filesToArchivePerNode, STAFArchiveModeEnum.TAR);

        // 3 Tars should have been created created
        Assert.assertEquals("3 Tars should have been created created", 3,
                            controller.getAllPreparedFilesToArchive().size());

        // Only 2 ready to send to staf.
        Set<AbstractPhysicalFile> tarToSendToStaf = controller.getAllPreparedFilesToArchive().stream()
                .filter(pf -> PhysicalFileStatusEnum.TO_STORE.equals(pf.getStatus())).collect(Collectors.toSet());
        Assert.assertEquals("2 Tars should be ready to send to STAF. Last one is not big enougth.", 2,
                            tarToSendToStaf.size());

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        Set<AbstractPhysicalFile> archivedFiles = controller.doArchivePreparedFiles(false);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // Check that 2 tars has been sent to STAF
        Assert.assertEquals("2 Tars should have beed stored into STAF System.", 2, archivedFiles.size());

        // Check that STAF Controller return all raw files has archived.
        Map<Path, URL> rawArchivedFiles = controller.getRawFilesArchived();
        rawArchivedFiles.forEach((p, u) -> System.out.println(String.format("%s --> %s", p.toString(), u.toString())));
        Assert.assertEquals("The 5 raw files to archive should have been stored in STAF system.", 5,
                            rawArchivedFiles.size());
        filesToArchiveWithoutInvalides
                .forEach(file -> Assert.assertTrue("Missing a stored file in STAFController raw files stored",
                                                   rawArchivedFiles.keySet().contains(file)));
        // Check that there is 5 tar files url
        Assert.assertEquals("There should be 5 files URL from STAF for the 5 raw files to archive", 5,
                            rawArchivedFiles.values().stream().distinct().collect(Collectors.toSet()).size());

    }

    /**
     * Testing the STAFController to check that files are well stored in TAR Mode {@link STAFArchiveModeEnum}
     * <ul>
     * <li>6 Files are to store</li>
     * <li>5 Files are accessible</li>
     * <li>1 File is not accessible</li>
     * <li>1 existing current TAR</li>
     * <li>Max tar size = 5000octets</li>
     * <li>Min tar size = 4500octets</li>
     * <li>TAR archiving hours limit = 50 hours</li>
     * </ul>
     * Results expected :
     * <ul>
     * <li>3 Tars prepared</li>
     * <li>3 Tars sent to STAF</li>
     * <li>0 Tar stored locally.</li>
     * <ul>
     *
     * @throws IOException
     * @throws STAFException
     * @throws URISyntaxException
     */
    @Test
    public void testStoreInTARModeWithExistingCurrentTAR() throws IOException, STAFException {

        // Init a tar current to test with existing files
        Path newTarCurrentPath = Paths
                .get(STAF_WORKSPACE_PATH.toString(), STAF_ARCHIVE_NAME, TARController.TAR_DIRECTORY, STAF_TEST_NODE,
                     String.format("%s_current", LocalDateTime.now()
                             .format(DateTimeFormatter.ofPattern(TARController.TAR_FILE_NAME_DATA_FORMAT))));
        Files.createDirectories(newTarCurrentPath);
        // Add a file in it
        Files.copy(filesToArchiveWithoutInvalides.stream().findFirst().get(),
                   Paths.get(newTarCurrentPath.toString(), "old_file.txt"));

        Map<String, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(STAF_TEST_NODE, filesToArchive);

        // Run STAF file preparation
        controller.prepareFilesToArchive(filesToArchivePerNode, STAFArchiveModeEnum.TAR);

        // 3 Tars should have been created created
        Assert.assertEquals("3 Tars should have been created created", 3,
                            controller.getAllPreparedFilesToArchive().size());

        // 3 Tars should be send to STAF
        Set<AbstractPhysicalFile> tarToSendToStaf = controller.getAllPreparedFilesToArchive().stream()
                .filter(pf -> PhysicalFileStatusEnum.TO_STORE.equals(pf.getStatus())).collect(Collectors.toSet());
        Assert.assertEquals("3 Tars shloud be ready to send to STAF. No remaining current TAR.", 3,
                            tarToSendToStaf.size());

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        Set<AbstractPhysicalFile> archivedFiles = controller.doArchivePreparedFiles(false);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // Check that 3 tars has been sent to STAF
        Assert.assertEquals("3 Tars should have been stored into STAF System.", 3, archivedFiles.size());

        // Check that STAF Controller return all raw files has archived.
        Map<Path, URL> rawArchivedFiles = controller.getRawFilesArchived();
        rawArchivedFiles.forEach((p, u) -> System.out.println(String.format("%s --> %s", p.toString(), u.toString())));
        Assert.assertEquals("The 5 raw files to archive should have been stored in STAF system.", 5,
                            rawArchivedFiles.size());
        filesToArchiveWithoutInvalides
                .forEach(file -> Assert.assertTrue("Missing a stored file in STAFController raw files stored",
                                                   rawArchivedFiles.keySet().contains(file)));
        // Check that there is 5 tar files url
        Assert.assertEquals("There should be 5 files URL from STAF for the 5 raw files to archive", 5,
                            rawArchivedFiles.values().stream().distinct().collect(Collectors.toSet()).size());

    }

    /**
     * Testing the STAFController to check that files are well stored in TAR Mode {@link STAFArchiveModeEnum}.<br/>
     * This test check that a current existing TAR is well handled and new file to store are aded to it.
     * <ul>
     * <li>6 Files are to store</li>
     * <li>5 Files are accessible</li>
     * <li>1 File is not accessible</li>
     * <li>1 existing current TAR. Old enought to be stored without size limit reached</li>
     * <li>Max tar size = 100000 octets</li>
     * <li>Min tar size = 20000 octets</li>
     * <li>TAR archiving hours limit = 50 hours</li>
     * </ul>
     * Results expected :
     * <ul>
     * <li>1 Tars prepared</li>
     * <li>1 Tars sent to STAF because of time limit reached.</li>
     * <li>0 Tar stored locally.</li>
     * <ul>
     *
     * @throws IOException
     * @throws STAFException
     * @throws URISyntaxException
     */
    @Test
    public void testStoreInTARModeWithExistingOldCurrentTAR() throws IOException, STAFException {

        // Set special limit to generate only one TAR thaht does not reach the threshold limit and the Max TAR size limit
        // The TAR should be created and send to STAF only because it is old enought.
        configuration.setTarSizeThreshold(20000L);
        configuration.setMaxTarSize(100000L);

        // Init a tar current old enought to send to STAF without max TAR size reached
        Path newTarCurrentPath = Paths
                .get(STAF_WORKSPACE_PATH.toString(), STAF_ARCHIVE_NAME, TARController.TAR_DIRECTORY, STAF_TEST_NODE,
                     String.format("%s_current", LocalDateTime.now()
                             .minusHours(configuration.getMaxTarArchivingHours() + 1).format(DateTimeFormatter
                                     .ofPattern(TARController.TAR_FILE_NAME_DATA_FORMAT))));
        Files.createDirectories(newTarCurrentPath);

        // Add a file in it
        Files.copy(filesToArchiveWithoutInvalides.stream().findFirst().get(),
                   Paths.get(newTarCurrentPath.toString(), "old_file.txt"));

        Map<String, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(STAF_TEST_NODE, filesToArchive);

        // Run STAF file preparation
        controller.prepareFilesToArchive(filesToArchivePerNode, STAFArchiveModeEnum.TAR);

        // 1 Tars should have been preapred
        Assert.assertEquals("1 Tar should have been created", 1, controller.getAllPreparedFilesToArchive().size());

        // 1 Tar should be send to STAF. The STAF is old enought to be sent to STAF.
        Set<AbstractPhysicalFile> tarToSendToStaf = controller.getAllPreparedFilesToArchive().stream()
                .filter(pf -> PhysicalFileStatusEnum.TO_STORE.equals(pf.getStatus())).collect(Collectors.toSet());
        Assert.assertEquals("1 Tar should be ready to send to STAF. No remaining current TAR.", 1,
                            tarToSendToStaf.size());

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        Set<AbstractPhysicalFile> archivedFiles = controller.doArchivePreparedFiles(false);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // Check that 3 tars has been sent to STAF
        Assert.assertEquals("1 Tar should have been stored into STAF System.", 1, archivedFiles.size());

        // Check that STAF Controller return all raw files has archived.
        Map<Path, URL> rawArchivedFiles = controller.getRawFilesArchived();
        rawArchivedFiles.forEach((p, u) -> System.out.println(String.format("%s --> %s", p.toString(), u.toString())));
        Assert.assertEquals("The 5 raw files to archive should have been stored in STAF system.", 5,
                            rawArchivedFiles.size());
        filesToArchiveWithoutInvalides
                .forEach(file -> Assert.assertTrue("Missing a stored file in STAFController raw files stored",
                                                   rawArchivedFiles.keySet().contains(file)));
        // Check that there is 5 tar files url
        Assert.assertEquals("There should be 5 files URL from STAF for the 5 raw files to archive", 5,
                            rawArchivedFiles.values().stream().distinct().collect(Collectors.toSet()).size());

    }

    /**
     * Testing the STAFController to check that files are well stored in TAR Mode {@link STAFArchiveModeEnum}.<br/>
     * This test check that a current existing TAR is well handled and new file to store are aded to it.
     * <ul>
     * <li>6 Files are to store</li>
     * <li>5 Files are accessible</li>
     * <li>1 File is not accessible</li>
     * <li>1 existing current TAR. Old enought to be stored without size limit reached</li>
     * <li>Max tar size = 100000 octets</li>
     * <li>Min tar size = 20000 octets</li>
     * <li>TAR archiving hours limit = 50 hours</li>
     * </ul>
     * Results expected :
     * <ul>
     * <li>1 Tars prepared</li>
     * <li>0 Tars sent to STAF because of time limit reached.</li>
     * <li>1 Tar stored locally.</li>
     * <ul>
     *
     * @throws IOException
     * @throws STAFException
     * @throws URISyntaxException
     */
    @Test
    public void testStoreInTARModeNotOldEnought() throws IOException, STAFException {

        // Set special limit to generate only one TAR thaht does not reach the threshold limit and the Max TAR size limit
        // The TAR should be created and send to STAF only because it is old enought.
        configuration.setTarSizeThreshold(50000L);
        configuration.setMaxTarSize(100000L);

        // Init a tar current old enought to send to STAF without max TAR size reached
        Path newTarCurrentPath = Paths
                .get(STAF_WORKSPACE_PATH.toString(), STAF_ARCHIVE_NAME, TARController.TAR_DIRECTORY, STAF_TEST_NODE,
                     String.format("%s_current", LocalDateTime.now()
                             .minusHours(configuration.getMaxTarArchivingHours() - 1).format(DateTimeFormatter
                                     .ofPattern(TARController.TAR_FILE_NAME_DATA_FORMAT))));
        Files.createDirectories(newTarCurrentPath);

        // Add a file in it
        Files.copy(filesToArchiveWithoutInvalides.stream().findFirst().get(),
                   Paths.get(newTarCurrentPath.toString(), "old_file.txt"));

        Map<String, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(STAF_TEST_NODE, filesToArchive);

        // Run STAF file preparation
        controller.prepareFilesToArchive(filesToArchivePerNode, STAFArchiveModeEnum.TAR);

        // 1 Tars should have been prepared
        Assert.assertEquals("One Tar should have been created", 1, controller.getAllPreparedFilesToArchive().size());

        // No Tar should be send to STAF. The TAR is not old enought and not big enought
        Set<AbstractPhysicalFile> tarToSendToStaf = controller.getAllPreparedFilesToArchive().stream()
                .filter(pf -> PhysicalFileStatusEnum.TO_STORE.equals(pf.getStatus())).collect(Collectors.toSet());
        Assert.assertEquals("No Tar should be ready to send to STAF. One remaining current TAR.", 0,
                            tarToSendToStaf.size());

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        Set<AbstractPhysicalFile> archivedFiles = controller.doArchivePreparedFiles(false);
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();

        // Check that 3 tars has been sent to STAF
        Assert.assertEquals("No Tar should have been stored into STAF System.", 0, archivedFiles.size());

        // Check that STAF Controller return all raw files has archived.
        Map<Path, URL> rawArchivedFiles = controller.getRawFilesArchived();
        rawArchivedFiles.forEach((p, u) -> System.out.println(String.format("%s --> %s", p.toString(), u.toString())));
        Assert.assertEquals("The 5 raw files to archive should have been stored in STAF system.", 5,
                            rawArchivedFiles.size());
        filesToArchiveWithoutInvalides
                .forEach(file -> Assert.assertTrue("Missing a stored file in STAFController raw files stored",
                                                   rawArchivedFiles.keySet().contains(file)));
        // Check that there is 5 tar files url
        Assert.assertEquals("There should be 5 files URL from STAF for the 5 raw files to archive", 5,
                            rawArchivedFiles.values().stream().distinct().collect(Collectors.toSet()).size());

    }

    /**
     * Testing the STAFController to check that files are well stored in CUT Mode {@link STAFArchiveModeEnum}.<br/>
     * <ul>
     * <li>6 Files are to store</li>
     * <li>5 Files are accessible</li>
     * <li>1 File is not accessible</li>
     * </ul>
     * Results expected :
     * <ul>
     * <li>20 Files are stored. 4parts per file.</li>
     * <ul>
     *
     * @throws IOException
     * @throws STAFException
     * @throws URISyntaxException
     */
    @Test
    public void testStoreInCutMode() throws IOException, STAFException {

        Map<String, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(STAF_TEST_NODE, filesToArchive);

        controller.prepareFilesToArchive(filesToArchivePerNode, STAFArchiveModeEnum.CUT);
        Assert.assertEquals(20, controller.getAllPreparedFilesToArchive().size());

        // All files are ready for transfer
        Set<AbstractPhysicalFile> tarToSendToStaf = controller.getAllPreparedFilesToArchive().stream()
                .filter(pf -> PhysicalFileStatusEnum.TO_STORE.equals(pf.getStatus())).collect(Collectors.toSet());
        Assert.assertEquals(20, tarToSendToStaf.size());

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        Set<AbstractPhysicalFile> archivedFiles = controller.doArchivePreparedFiles(false);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        // 20 files to archive. Max number of files per archive session = 10 -> 2xarchive command
        Mockito.verify(stafSessionMock, Mockito.times(2)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // Check that the number of files archived in STAF is 5(files)*4(parts per file) = 20(cuted files).
        Assert.assertEquals(20, archivedFiles.size());

        // Check that STAF Controller return all raw files has archived.
        Map<Path, URL> rawArchivedFiles = controller.getRawFilesArchived();
        rawArchivedFiles.forEach((p, u) -> System.out.println(String.format("%s --> %s", p.toString(), u.toString())));
        Assert.assertEquals(5, rawArchivedFiles.size());
        filesToArchiveWithoutInvalides
                .forEach(file -> Assert.assertTrue("Missing a stored file in STAFController raw files stored",
                                                   rawArchivedFiles.keySet().contains(file)));

        // Check that there is 5 file url for the 5 files stored.
        Assert.assertEquals("There should be 5 files URL from STAF for the 5 raw files to archive", 5,
                            rawArchivedFiles.values().stream().distinct().collect(Collectors.toSet()).size());

    }

    /**
     * Testing restoration file with STAF Controller.
     * <ul>
     * <li> Restoring a file with staf url : staf://ARCHIVE_TEST/node/test/file.txt</li>
     * </ul>
     * Results exptected :
     * <ul>
     * <li>1 staff command stafreretrieve sent to the ARCHIVE_TEST archive with the path node/test/file.txt</li>
     * </ul>
     * @throws MalformedURLException
     * @throws STAFException
     */
    @Test
    public void testRestoreFiles() throws MalformedURLException, STAFException {

        Set<URL> stafUrls = Sets.newHashSet();
        Map<String, String> restoreParameters = Maps.newHashMap();

        URL stafUrl = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME,
                Paths.get(STAF_TEST_NODE, "file.txt").toString());
        stafUrls.add(stafUrl);
        restoreParameters.put(stafUrl.getPath().toString(),
                              Paths.get(STAF_WORKSPACE_PATH.toString(), "file.txt").toString());

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilRetrieveBuffered(Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        controller.restoreFiles(stafUrls, STAF_WORKSPACE_PATH);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilRetrieveBuffered(restoreParameters);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

    }

    /**
     * Testing restoration file with STAF Controller.
     * <ul>
     * <li> Restoring a file with staf url : staf://ARCHIVE_TEST/node/test/file.txt?parts=3</li>
     * </ul>
     * Results exptected :
     * <ul>
     * <li>3 staff command stafreretrieve sent to the ARCHIVE_TEST archive with paths : node/test/file.txt_00, node/test/file.txt_01 and node/test/file.txt_02</li>
     * </ul>
     * @throws MalformedURLException
     * @throws STAFException
     */
    @Test
    public void testRestoreCutedFiles() throws MalformedURLException, STAFException {

        Set<URL> stafUrls = Sets.newHashSet();
        Map<String, String> restoreParameters = Maps.newHashMap();

        URL stafCutUrl = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME,
                Paths.get(STAF_TEST_NODE,
                          "cuted_file.txt?" + STAFUrlParameter.CUT_PARTS_PARAMETER.getParameterName() + "=3")
                        .toString());
        stafUrls.add(stafCutUrl);
        restoreParameters.put(stafCutUrl.getPath().toString() + "_00",
                              Paths.get(STAF_WORKSPACE_PATH.toString(), "cuted_file.txt_00").toString());
        restoreParameters.put(stafCutUrl.getPath().toString() + "_01",
                              Paths.get(STAF_WORKSPACE_PATH.toString(), "cuted_file.txt_01").toString());
        restoreParameters.put(stafCutUrl.getPath().toString() + "_02",
                              Paths.get(STAF_WORKSPACE_PATH.toString(), "cuted_file.txt_02").toString());

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilRetrieveBuffered(Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        controller.restoreFiles(stafUrls, STAF_WORKSPACE_PATH);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilRetrieveBuffered(restoreParameters);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // TODO : Check concatenation of 3 restored files into 1.

    }

    /**
     * Testing restoration file with STAF Controller.
     * <ul>
     * <li> Restoring a file with staf url : staf://ARCHIVE_TEST/node/test/file.tar?filename=file.txt</li>
     * </ul>
     * Results exptected :
     * <ul>
     * <li>1 staff command stafreretrieve sent to the ARCHIVE_TEST archive with paths : node/test/file.tar</li>
     * </ul>
     * @throws MalformedURLException
     * @throws STAFException
     */
    @Test
    public void testRestoreTARFiles() throws MalformedURLException, STAFException {

        Set<URL> stafUrls = Sets.newHashSet();
        Map<String, String> restoreParameters = Maps.newHashMap();

        // Three STAF Url , two from the same file TAR but for two differents files in it and one from an other TAR file.
        URL stafCutUrl = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME,
                Paths.get(STAF_TEST_NODE,
                          "file.tar?" + STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName() + "=file.txt")
                        .toString());
        stafUrls.add(stafCutUrl);
        URL stafCutUrl2 = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME,
                Paths.get(STAF_TEST_NODE,
                          "file.tar?" + STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName() + "=file2.txt")
                        .toString());
        stafUrls.add(stafCutUrl2);
        URL stafCutUrl3 = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME,
                Paths.get(STAF_TEST_NODE,
                          "file2.tar?" + STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName() + "=file2.txt")
                        .toString());
        stafUrls.add(stafCutUrl3);

        // Only two tar should be retreived from the STAF.
        restoreParameters.put(stafCutUrl.getPath().toString(),
                              Paths.get(STAF_WORKSPACE_PATH.toString(), "file.tar").toString());
        restoreParameters.put(stafCutUrl3.getPath().toString(),
                              Paths.get(STAF_WORKSPACE_PATH.toString(), "file2.tar").toString());

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilRetrieveBuffered(Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        controller.restoreFiles(stafUrls, STAF_WORKSPACE_PATH);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilRetrieveBuffered(restoreParameters);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // TODO : Check extraction of wanted file.

    }

}
