/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.staf.domain.AbstractPhysicalFile;
import fr.cnes.regards.framework.staf.domain.PhysicalFileStatusEnum;
import fr.cnes.regards.framework.staf.domain.STAFArchive;
import fr.cnes.regards.framework.staf.domain.STAFArchiveModeEnum;
import fr.cnes.regards.framework.staf.domain.STAFConfiguration;
import fr.cnes.regards.framework.staf.event.IClientCollectListener;
import fr.cnes.regards.framework.staf.exception.STAFException;
import fr.cnes.regards.framework.staf.mock.STAFMock;
import fr.cnes.regards.framework.staf.protocol.STAFUrlFactory;
import fr.cnes.regards.framework.staf.protocol.STAFUrlParameter;

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

    private final static Path RESTORE_DIRECTORY_PATH = Paths.get(new File("target/STAF/restore").getAbsolutePath());

    @BeforeClass
    public static void initAll() throws IOException {
        STAFUrlFactory.initSTAFURLProtocol();
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

        if (Files.exists(STAF_WORKSPACE_PATH)) {
            Files.setPosixFilePermissions(STAF_WORKSPACE_PATH,
                                          Sets.newHashSet(PosixFilePermission.OWNER_EXECUTE,
                                                          PosixFilePermission.OWNER_READ,
                                                          PosixFilePermission.OWNER_WRITE));
            FileUtils.deleteDirectory(STAF_WORKSPACE_PATH.toFile());
        }
        // Init directories
        if (Files.exists(RESTORE_DIRECTORY_PATH)) {
            Files.setPosixFilePermissions(RESTORE_DIRECTORY_PATH,
                                          Sets.newHashSet(PosixFilePermission.OWNER_EXECUTE,
                                                          PosixFilePermission.OWNER_READ,
                                                          PosixFilePermission.OWNER_WRITE));
            FileUtils.deleteDirectory(RESTORE_DIRECTORY_PATH.toFile());
        }
        Files.createDirectories(RESTORE_DIRECTORY_PATH);

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
        STAFSessionManager stafManagerMock = Mockito.mock(STAFSessionManager.class);

        Mockito.when(stafManagerMock.getConfiguration()).thenReturn(configuration);
        Mockito.when(stafManagerMock.getReservation(Mockito.any())).thenReturn(0);
        // Simulate archive is ok for all files.
        Mockito.when(stafSessionMock.staffilArchive(Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
                .thenAnswer(pInvocation -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> files = pInvocation.getArgumentAt(0, Map.class);
                    return Lists.newArrayList(files.keySet());
                });
        List<Integer> sessions = Lists.newArrayList();
        sessions.add(0);
        Mockito.when(stafManagerMock.getReservations()).thenReturn(sessions);
        Mockito.when(stafManagerMock.getNewSession()).thenReturn(stafSessionMock);

        stafService = new STAFService(stafManagerMock, stafArchive);

        controller = new STAFController(configuration, STAF_WORKSPACE_PATH, stafService);
        controller.initializeWorkspaceDirectories();

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

        // Force use of store in normal mode by setting file max size limit low.
        configuration.setMinFileSize(1000L);
        configuration.setMaxFileSize(10000L);

        Map<Path, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(Paths.get(STAF_TEST_NODE), filesToArchive);

        Set<AbstractPhysicalFile> preparedFiles = controller.prepareFilesToArchive(filesToArchivePerNode);
        Assert.assertEquals(5, preparedFiles.size());

        // All files are ready for transfer
        Set<AbstractPhysicalFile> tarToSendToStaf = preparedFiles.stream()
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
        Set<AbstractPhysicalFile> archivedFiles = controller.archiveFiles(preparedFiles, Paths.get(STAF_TEST_NODE),
                                                                          false);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilArchive(localFileToArchiveMap, "CS1", false);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        Assert.assertEquals(5, archivedFiles.size());

        // Check that STAF Controller return all raw files has archived.
        Map<Path, URL> rawArchivedFiles = controller.getRawFilesArchived(preparedFiles);
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
        Map<Path, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(Paths.get(STAF_TEST_NODE), filesToArchive);

        // Run STAF file preparation
        Set<AbstractPhysicalFile> preparedFiles = controller.prepareFilesToArchive(filesToArchivePerNode);

        // 3 Tars should have been created created
        Assert.assertEquals("3 Tars should have been created created", 3, preparedFiles.size());

        // Only 2 ready to send to staf.
        Set<AbstractPhysicalFile> tarToSendToStaf = preparedFiles.stream()
                .filter(pf -> PhysicalFileStatusEnum.TO_STORE.equals(pf.getStatus())).collect(Collectors.toSet());
        Assert.assertEquals("2 Tars should be ready to send to STAF. Last one is not big enougth.", 2,
                            tarToSendToStaf.size());

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        Set<AbstractPhysicalFile> archivedFiles = controller.archiveFiles(preparedFiles, Paths.get(STAF_TEST_NODE),
                                                                          false);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // Check that 2 tars has been sent to STAF
        Assert.assertEquals("2 Tars should have beed stored into STAF System.", 2, archivedFiles.size());

        // Check that STAF Controller return all raw files has archived.
        Map<Path, URL> rawArchivedFiles = controller.getRawFilesArchived(preparedFiles);
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

        Map<Path, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(Paths.get(STAF_TEST_NODE), filesToArchive);

        // Run STAF file preparation
        Set<AbstractPhysicalFile> preparedFiles = controller.prepareFilesToArchive(filesToArchivePerNode);

        // 3 Tars should have been created created
        Assert.assertEquals("3 Tars should have been created created", 3, preparedFiles.size());

        // 3 Tars should be send to STAF
        Set<AbstractPhysicalFile> tarToSendToStaf = preparedFiles.stream()
                .filter(pf -> PhysicalFileStatusEnum.TO_STORE.equals(pf.getStatus())).collect(Collectors.toSet());
        Assert.assertEquals("3 Tars shloud be ready to send to STAF. No remaining current TAR.", 3,
                            tarToSendToStaf.size());

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        Set<AbstractPhysicalFile> archivedFiles = controller.archiveFiles(preparedFiles, Paths.get(STAF_TEST_NODE),
                                                                          false);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // Check that 3 tars has been sent to STAF
        Assert.assertEquals("3 Tars should have been stored into STAF System.", 3, archivedFiles.size());

        // Check that STAF Controller return all raw files has archived.
        Map<Path, URL> rawArchivedFiles = controller.getRawFilesArchived(preparedFiles);
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

        Map<Path, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(Paths.get(STAF_TEST_NODE), filesToArchive);

        // Run STAF file preparation
        Set<AbstractPhysicalFile> preparedFiles = controller.prepareFilesToArchive(filesToArchivePerNode);

        // 1 Tars should have been preapred
        Assert.assertEquals("1 Tar should have been created", 1, preparedFiles.size());

        // 1 Tar should be send to STAF. The STAF is old enought to be sent to STAF.
        Set<AbstractPhysicalFile> tarToSendToStaf = preparedFiles.stream()
                .filter(pf -> PhysicalFileStatusEnum.TO_STORE.equals(pf.getStatus())).collect(Collectors.toSet());
        Assert.assertEquals("1 Tar should be ready to send to STAF. No remaining current TAR.", 1,
                            tarToSendToStaf.size());

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        Set<AbstractPhysicalFile> archivedFiles = controller.archiveFiles(preparedFiles, Paths.get(STAF_TEST_NODE),
                                                                          false);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // Check that 3 tars has been sent to STAF
        Assert.assertEquals("1 Tar should have been stored into STAF System.", 1, archivedFiles.size());

        // Check that STAF Controller return all raw files has archived.
        Map<Path, URL> rawArchivedFiles = controller.getRawFilesArchived(preparedFiles);
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

        Map<Path, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(Paths.get(STAF_TEST_NODE), filesToArchive);

        // Run STAF file preparation
        Set<AbstractPhysicalFile> preparedFiles = controller.prepareFilesToArchive(filesToArchivePerNode);

        // 1 Tars should have been prepared
        Assert.assertEquals("One Tar should have been created", 1, preparedFiles.size());

        // No Tar should be send to STAF. The TAR is not old enought and not big enought
        Set<AbstractPhysicalFile> tarToSendToStaf = preparedFiles.stream()
                .filter(pf -> PhysicalFileStatusEnum.TO_STORE.equals(pf.getStatus())).collect(Collectors.toSet());
        Assert.assertEquals("No Tar should be ready to send to STAF. One remaining current TAR.", 0,
                            tarToSendToStaf.size());

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        Set<AbstractPhysicalFile> archivedFiles = controller.archiveFiles(preparedFiles, Paths.get(STAF_TEST_NODE),
                                                                          false);
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();

        // Check that 3 tars has been sent to STAF
        Assert.assertEquals("No Tar should have been stored into STAF System.", 0, archivedFiles.size());

        // Check that STAF Controller return all raw files has archived.
        Map<Path, URL> rawArchivedFiles = controller.getRawFilesArchived(preparedFiles);
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

        // Force use of store in cut mode by setting file max size limit low.
        configuration.setMinFileSize(10L);
        configuration.setMaxFileSize(1000L);
        Map<Path, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(Paths.get(STAF_TEST_NODE), filesToArchive);

        Set<AbstractPhysicalFile> preparedFiles = controller.prepareFilesToArchive(filesToArchivePerNode);
        Assert.assertEquals(20, preparedFiles.size());

        // All files are ready for transfer
        Set<AbstractPhysicalFile> tarToSendToStaf = preparedFiles.stream()
                .filter(pf -> PhysicalFileStatusEnum.TO_STORE.equals(pf.getStatus())).collect(Collectors.toSet());
        Assert.assertEquals(20, tarToSendToStaf.size());

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        Set<AbstractPhysicalFile> archivedFiles = controller.archiveFiles(preparedFiles, Paths.get(STAF_TEST_NODE),
                                                                          false);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        // 20 files to archive. Max number of files per archive session = 10 -> 2xarchive command
        Mockito.verify(stafSessionMock, Mockito.times(2)).staffilArchive(Mockito.anyMapOf(String.class, String.class),
                                                                         Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // Check that the number of files archived in STAF is 5(files)*4(parts per file) = 20(cuted files).
        Assert.assertEquals(20, archivedFiles.size());

        // Check that STAF Controller return all raw files has archived.
        Map<Path, URL> rawArchivedFiles = controller.getRawFilesArchived(preparedFiles);
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

        String fileNameToRestore = "file.txt";
        Set<URL> stafUrls = Sets.newHashSet();
        Map<String, String> restoreParameters = Maps.newHashMap();

        URL stafUrl = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME,
                Paths.get(STAF_TEST_NODE, "file.txt").toString());
        stafUrls.add(stafUrl);
        restoreParameters.put(stafUrl.getPath().toString(),
                              Paths.get(RESTORE_DIRECTORY_PATH.toString(), fileNameToRestore).toString());

        // Simulate STAF Files restitution
        Mockito.doAnswer(invocation -> STAFMock.mockRestoration(invocation)).when(stafSessionMock)
                .staffilRetrieveBuffered(Mockito.any());

        IClientCollectListener listenerMock = Mockito.mock(IClientCollectListener.class);

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilRetrieveBuffered(Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        controller.restoreFiles(controller.prepareFilesToRestore(stafUrls), RESTORE_DIRECTORY_PATH, listenerMock);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilRetrieveBuffered(restoreParameters);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // Check event sent after staf retreive.
        Path resultFile = Paths.get(RESTORE_DIRECTORY_PATH.toString(), fileNameToRestore);
        ArgumentCaptor<URL> argumentURL = ArgumentCaptor.forClass(URL.class);
        ArgumentCaptor<Path> argumentPath = ArgumentCaptor.forClass(Path.class);
        Mockito.verify(listenerMock, Mockito.times(1)).fileRetreived(argumentURL.capture(), argumentPath.capture());
        Mockito.verify(listenerMock, Mockito.times(0)).fileRetrieveError(Mockito.any());
        Assert.assertEquals(true, argumentURL.getValue().equals(stafUrl));
        Assert.assertEquals(true, argumentPath.getValue()
                .equals(Paths.get(RESTORE_DIRECTORY_PATH.toString(), fileNameToRestore)));
        Assert.assertTrue("Normal file restoration error. File does not exits !", resultFile.toFile().exists());

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

        // Set special configuration to force use of only one staf session.
        // Set the maximum number of sessions
        configuration.setMaxSessionsRestitutionMode(10);
        // Set the maximum number of stream per sesssion
        configuration.setMaxSessionStreamsRestitutionMode(10);
        // Set the maximum number of files per stream
        configuration.setMaxStreamFilesRestitutionMode(10);

        String cutfileName = "big_file_test_1.txt";
        Set<URL> stafUrls = Sets.newHashSet();
        Map<String, String> restoreParameters = Maps.newHashMap();

        int numberOfParts = 9;
        URL stafCutUrl = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME, Paths
                .get(STAF_TEST_NODE,
                     cutfileName + "?" + STAFUrlParameter.CUT_PARTS_PARAMETER.getParameterName() + "=" + numberOfParts)
                .toString());
        stafUrls.add(stafCutUrl);
        for (int i = 0; i < numberOfParts; i++) {
            restoreParameters.put(stafCutUrl.getPath().toString() + "_0" + i,
                                  Paths.get(RESTORE_DIRECTORY_PATH.toString(), cutfileName + "_0" + i).toString());
        }

        // Simulate STAF Files restitution
        Mockito.doAnswer(invocation -> STAFMock.mockRestoration(invocation)).when(stafSessionMock)
                .staffilRetrieveBuffered(Mockito.any());

        IClientCollectListener listenerMock = Mockito.mock(IClientCollectListener.class);

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilRetrieveBuffered(Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        controller.restoreFiles(controller.prepareFilesToRestore(stafUrls), RESTORE_DIRECTORY_PATH, listenerMock);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilRetrieveBuffered(restoreParameters);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // Check event sent after staf retreive.
        Path resultFile = Paths.get(RESTORE_DIRECTORY_PATH.toString(), cutfileName);
        ArgumentCaptor<URL> argumentURL = ArgumentCaptor.forClass(URL.class);
        ArgumentCaptor<Path> argumentPath = ArgumentCaptor.forClass(Path.class);
        Mockito.verify(listenerMock, Mockito.times(1)).fileRetreived(argumentURL.capture(), argumentPath.capture());
        Mockito.verify(listenerMock, Mockito.times(0)).fileRetrieveError(Mockito.any());
        Assert.assertEquals(true, argumentURL.getValue().equals(stafCutUrl));
        Assert.assertEquals(true,
                            argumentPath.getValue().equals(Paths.get(RESTORE_DIRECTORY_PATH.toString(), cutfileName)));
        Assert.assertTrue("Cut file restoration error. File does not exits !", resultFile.toFile().exists());
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
        String fileName = "file.txt";
        URL stafTarUrl = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME,
                Paths.get(STAF_TEST_NODE,
                          "file.tar?" + STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName() + "=" + fileName)
                        .toString());
        Path exptectedFile = Paths.get(RESTORE_DIRECTORY_PATH.toString(), fileName);
        stafUrls.add(stafTarUrl);

        String fileName2 = "file2.txt";
        URL stafTarUrl2 = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME,
                Paths.get(STAF_TEST_NODE,
                          "file.tar?" + STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName() + "=" + fileName2)
                        .toString());
        Path exptectedFile2 = Paths.get(RESTORE_DIRECTORY_PATH.toString(), fileName2);
        stafUrls.add(stafTarUrl2);
        URL stafTarUrl3 = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME,
                Paths.get(STAF_TEST_NODE,
                          "file2.tar?" + STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName() + "=" + fileName2)
                        .toString());
        Path exptectedFile3 = Paths.get(RESTORE_DIRECTORY_PATH.toString(), "file2_1.txt");
        stafUrls.add(stafTarUrl3);

        // Only two tar should be retreived from the STAF.
        restoreParameters.put(stafTarUrl.getPath().toString(),
                              Paths.get(RESTORE_DIRECTORY_PATH.toString(), "file.tar").toString());
        restoreParameters.put(stafTarUrl3.getPath().toString(),
                              Paths.get(RESTORE_DIRECTORY_PATH.toString(), "file2.tar").toString());

        // Simulate STAF Files restitution
        Mockito.doAnswer(invocation -> STAFMock.mockRestoration(invocation)).when(stafSessionMock)
                .staffilRetrieveBuffered(Mockito.any());

        IClientCollectListener listenerMock = Mockito.mock(IClientCollectListener.class);

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilRetrieveBuffered(Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        controller.restoreFiles(controller.prepareFilesToRestore(stafUrls), RESTORE_DIRECTORY_PATH, listenerMock);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilRetrieveBuffered(restoreParameters);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // Check event sent after staf retreive.
        ArgumentCaptor<URL> argumentURL = ArgumentCaptor.forClass(URL.class);
        ArgumentCaptor<Path> argumentPath = ArgumentCaptor.forClass(Path.class);
        Mockito.verify(listenerMock, Mockito.times(3)).fileRetreived(argumentURL.capture(), argumentPath.capture());
        Mockito.verify(listenerMock, Mockito.times(0)).fileRetrieveError(Mockito.any());
        for (URL url : stafUrls) {
            Assert.assertEquals(true, argumentURL.getAllValues().contains(url));
        }
        Assert.assertTrue(String.format("Event for file %s successfully retrieved not sent", exptectedFile),
                          argumentPath.getAllValues().contains(exptectedFile));
        Assert.assertTrue(String.format("Event for file %s successfully retrieved not sent", exptectedFile2),
                          argumentPath.getAllValues().contains(exptectedFile2));
        Assert.assertTrue(String.format("Event for file %s successfully retrieved not sent", exptectedFile3),
                          argumentPath.getAllValues().contains(exptectedFile3));

        // Check files extracted and restore are available
        for (Path resultFile : argumentPath.getAllValues()) {
            Assert.assertTrue(String.format("Error restored file does not exists %s", resultFile.toString()),
                              resultFile.toFile().exists());
        }
    }

    /**
     * If files are not present in STAF, the STAFController have to send an error notification for each file.
     * @throws MalformedURLException
     * @throws STAFException
     */
    @Test
    public void testRestoreWithFileNotFound() throws MalformedURLException, STAFException {

        String fileNameToRestore = "file.txt";
        Set<URL> stafUrls = Sets.newHashSet();
        Map<String, String> restoreParameters = Maps.newHashMap();

        // Add a normal file to retrieve
        URL stafUrl = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME,
                Paths.get(STAF_TEST_NODE, "file.txt").toString());
        stafUrls.add(stafUrl);
        restoreParameters.put(stafUrl.getPath().toString(),
                              Paths.get(RESTORE_DIRECTORY_PATH.toString(), fileNameToRestore).toString());

        // Add a file into a TAR to retrieve
        String fileName = "fileInTar.txt";
        URL stafTarUrl = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME,
                Paths.get(STAF_TEST_NODE,
                          "file.tar?" + STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName() + "=" + fileName)
                        .toString());
        restoreParameters.put(stafTarUrl.getPath().toString(),
                              Paths.get(RESTORE_DIRECTORY_PATH.toString(), "file.tar").toString());
        stafUrls.add(stafTarUrl);

        // Aadd a cuted file to retrieve
        int numberOfParts = 9;
        String cutfileName = "big_file_test_1.txt";
        URL stafCutUrl = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME, Paths
                .get(STAF_TEST_NODE,
                     cutfileName + "?" + STAFUrlParameter.CUT_PARTS_PARAMETER.getParameterName() + "=" + numberOfParts)
                .toString());
        stafUrls.add(stafCutUrl);
        for (int i = 0; i < numberOfParts; i++) {
            restoreParameters.put(stafCutUrl.getPath().toString() + "_0" + i,
                                  Paths.get(RESTORE_DIRECTORY_PATH.toString(), cutfileName + "_0" + i).toString());
        }

        // Simulate STAF Files restitution
        Mockito.doAnswer(invocation -> {
            throw new STAFException("STAF Error simulation");
        }).when(stafSessionMock).staffilRetrieveBuffered(Mockito.any());

        IClientCollectListener listenerMock = Mockito.mock(IClientCollectListener.class);

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilRetrieveBuffered(Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        controller.restoreFiles(controller.prepareFilesToRestore(stafUrls), RESTORE_DIRECTORY_PATH, listenerMock);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilRetrieveBuffered(restoreParameters);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // Check event sent after staf retreive.
        ArgumentCaptor<URL> argumentURL = ArgumentCaptor.forClass(URL.class);
        Mockito.verify(listenerMock, Mockito.times(0)).fileRetreived(Mockito.any(), Mockito.any());
        Mockito.verify(listenerMock, Mockito.times(3)).fileRetrieveError(argumentURL.capture());
        Assert.assertEquals(true, argumentURL.getAllValues().contains(stafUrl));
        Assert.assertEquals(true, argumentURL.getAllValues().contains(stafTarUrl));
        Assert.assertEquals(true, argumentURL.getAllValues().contains(stafCutUrl));
    }

    /**
     * Test restoring files with the 3 archive mode {@link STAFArchiveModeEnum} in the same STAF session.
     * @throws MalformedURLException
     * @throws STAFException
     */
    @Test
    public void testRestoreMultiTypes() throws MalformedURLException, STAFException {

        Set<URL> stafUrls = Sets.newHashSet();
        Set<URL> expectedRestoredSTAFUrls = Sets.newHashSet();
        Map<String, String> restoreParameters = Maps.newHashMap();

        // Add a normal file to retrieve
        String fileName = "file.txt";
        URL stafUrl = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME,
                Paths.get(STAF_TEST_NODE, fileName).toString());
        stafUrls.add(stafUrl);
        expectedRestoredSTAFUrls.add(stafUrl);
        restoreParameters.put(stafUrl.getPath().toString(),
                              Paths.get(RESTORE_DIRECTORY_PATH.toString(), fileName).toString());
        Path expectedNormalFile = Paths.get(RESTORE_DIRECTORY_PATH.toString(), fileName);

        // Add two file into a TAR to retrieve. One exists, the other one is not present in the TAR.
        String fileNotInTarName = "fileInTar.txt";
        URL stafTarUrl = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME, Paths
                .get(STAF_TEST_NODE,
                     "file.tar?" + STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName() + "=" + fileNotInTarName)
                .toString());
        restoreParameters.put(stafTarUrl.getPath().toString(),
                              Paths.get(RESTORE_DIRECTORY_PATH.toString(), "file.tar").toString());
        stafUrls.add(stafTarUrl);
        String fileInTarName = "file2.txt";
        URL stafTarUrl2 = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME, Paths
                .get(STAF_TEST_NODE,
                     "file.tar?" + STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName() + "=" + fileInTarName)
                .toString());
        restoreParameters.put(stafTarUrl2.getPath().toString(),
                              Paths.get(RESTORE_DIRECTORY_PATH.toString(), "file.tar").toString());
        stafUrls.add(stafTarUrl2);
        expectedRestoredSTAFUrls.add(stafTarUrl2);
        Path expectedFileInTar = Paths.get(RESTORE_DIRECTORY_PATH.toString(), fileInTarName);

        // Aadd a cuted file to retrieve
        int numberOfParts = 9;
        String cutfileName = "big_file_test_1.txt";
        URL stafCutUrl = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME, Paths
                .get(STAF_TEST_NODE,
                     cutfileName + "?" + STAFUrlParameter.CUT_PARTS_PARAMETER.getParameterName() + "=" + numberOfParts)
                .toString());
        stafUrls.add(stafCutUrl);
        expectedRestoredSTAFUrls.add(stafCutUrl);
        for (int i = 0; i < numberOfParts; i++) {
            restoreParameters.put(stafCutUrl.getPath().toString() + "_0" + i,
                                  Paths.get(RESTORE_DIRECTORY_PATH.toString(), cutfileName + "_0" + i).toString());
        }
        Path expectedCutFile = Paths.get(RESTORE_DIRECTORY_PATH.toString(), cutfileName);

        // Simulate STAF Files restitution
        Mockito.doAnswer(invocation -> STAFMock.mockRestoration(invocation)).when(stafSessionMock)
                .staffilRetrieveBuffered(Mockito.any());

        IClientCollectListener listenerMock = Mockito.mock(IClientCollectListener.class);

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilRetrieveBuffered(Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        controller.restoreFiles(controller.prepareFilesToRestore(stafUrls), RESTORE_DIRECTORY_PATH, listenerMock);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilRetrieveBuffered(restoreParameters);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        ArgumentCaptor<URL> argumentURL = ArgumentCaptor.forClass(URL.class);
        ArgumentCaptor<URL> argumentURLNotFound = ArgumentCaptor.forClass(URL.class);
        ArgumentCaptor<Path> argumentPath = ArgumentCaptor.forClass(Path.class);
        Mockito.verify(listenerMock, Mockito.times(1)).fileRetrieveError(argumentURLNotFound.capture());
        Mockito.verify(listenerMock, Mockito.times(3)).fileRetreived(argumentURL.capture(), argumentPath.capture());
        for (URL url : expectedRestoredSTAFUrls) {
            Assert.assertEquals(String.format("Missing a notification on the listener for URL %s", url.toString()),
                                true, argumentURL.getAllValues().contains(url));
        }
        Assert.assertEquals("As the file fileInTar.txt is not in the mocked TAR, the fileRetrieveError of the listener should be called with the URL of the missing file",
                            true, argumentURLNotFound.getValue().equals(stafTarUrl));
        Assert.assertTrue(String.format("Event for Normal file %s successfully retrieved not sent", expectedNormalFile),
                          argumentPath.getAllValues().contains(expectedNormalFile));
        Assert.assertTrue(String.format("Event for file in TAR %s successfully retrieved not sent", expectedFileInTar),
                          argumentPath.getAllValues().contains(expectedFileInTar));
        Assert.assertTrue(String.format("Event for Cuted file %s successfully retrieved not sent", expectedCutFile),
                          argumentPath.getAllValues().contains(expectedCutFile));

        // Check files extracted and restore are available
        for (Path resultFile : argumentPath.getAllValues()) {
            Assert.assertTrue(String.format("Error restored file does not exists %s", resultFile.toString()),
                              resultFile.toFile().exists());
        }
    }

    /**
     * If the restoration directory is not accessible, the STAFController ave to send a error notification.
     * @throws IOException
     * @throws STAFException
     */
    @Test
    public void testRestoreWithAccessDeniedToRestoreDirectory() throws IOException, STAFException {

        // Simulate access denied to restore directory
        if (Files.exists(RESTORE_DIRECTORY_PATH)) {
            Files.setPosixFilePermissions(RESTORE_DIRECTORY_PATH,
                                          Sets.newHashSet(PosixFilePermission.OWNER_EXECUTE,
                                                          PosixFilePermission.OWNER_READ,
                                                          PosixFilePermission.OWNER_WRITE));
            FileUtils.deleteDirectory(RESTORE_DIRECTORY_PATH.toFile());
        }
        Files.createDirectories(RESTORE_DIRECTORY_PATH);
        Files.setPosixFilePermissions(RESTORE_DIRECTORY_PATH, Sets.newHashSet());

        String fileNameToRestore = "file.txt";
        Set<URL> stafUrls = Sets.newHashSet();
        Map<String, String> restoreParameters = Maps.newHashMap();

        URL stafUrl = new URL(STAFUrlFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME,
                Paths.get(STAF_TEST_NODE, "file.txt").toString());
        stafUrls.add(stafUrl);
        restoreParameters.put(stafUrl.getPath().toString(),
                              Paths.get(RESTORE_DIRECTORY_PATH.toString(), fileNameToRestore).toString());

        // Simulate STAF Files restitution
        Mockito.doAnswer(invocation -> STAFMock.mockRestoration(invocation)).when(stafSessionMock)
                .staffilRetrieveBuffered(Mockito.any());

        IClientCollectListener listenerMock = Mockito.mock(IClientCollectListener.class);

        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconOpen(Mockito.any(), Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).staffilRetrieveBuffered(Mockito.any());
        Mockito.verify(stafSessionMock, Mockito.times(0)).stafconClose();
        controller.restoreFiles(controller.prepareFilesToRestore(stafUrls), RESTORE_DIRECTORY_PATH, listenerMock);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconOpen(STAF_ARCHIVE_NAME, STAF_ARCHIVE_PASSWORD);
        Mockito.verify(stafSessionMock, Mockito.times(1)).staffilRetrieveBuffered(restoreParameters);
        Mockito.verify(stafSessionMock, Mockito.times(1)).stafconClose();

        // Check event sent after staf retreive.
        ArgumentCaptor<URL> argumentURL = ArgumentCaptor.forClass(URL.class);
        Mockito.verify(listenerMock, Mockito.times(0)).fileRetreived(Mockito.any(), Mockito.any());
        Mockito.verify(listenerMock, Mockito.times(1)).fileRetrieveError(argumentURL.capture());
        Assert.assertEquals("As the restoration directory is not available, there should be an error notification sent for the STAF URL",
                            true, argumentURL.getValue().equals(stafUrl));
    }

}
