package fr.cnes.regards.modules.storage.plugins.staf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
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

    @Before
    public void init() throws STAFException, IOException {

        FileUtils.deleteDirectory(Paths.get("target/workspace").toFile());

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

        // TODO : Use of two limits max and threshold
        configuration.setTarSizeThreshold(5000L);
        configuration.setMaxTarSize(5000L);
        configuration.setMaxTarArchivingHours(5L);

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

        controller = new STAFController(configuration, Paths.get("target/workspace"), stafService);

    }

    @Test
    public void testPrepareFilesToStore() throws IOException, STAFException, URISyntaxException {

        Map<String, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(STAF_TEST_NODE, filesToArchive);

        controller.prepareFilesToArchive(filesToArchivePerNode, STAFArchiveModeEnum.NORMAL);
        Assert.assertEquals(5, controller.getAllFilesToArchive().size());

        // All files are ready for transfer
        Set<AbstractPhysicalFile> tarToSendToStaf = controller.getAllFilesToArchive().stream()
                .filter(pf -> PhysicalFileStatusEnum.TO_STORE.equals(pf.getStatus())).collect(Collectors.toSet());
        Assert.assertEquals(5, tarToSendToStaf.size());

        // Construct map of files to archive to test staffFileArchive method call
        Map<String, String> localFileToArchiveMap = Maps.newHashMap();
        localFileToArchiveMap.put("src/test/resources/staf/income/file_test_3.txt",
                                  "/test/node/eadcc622739d58e8a78170b67c6ff9f5");
        localFileToArchiveMap.put("src/test/resources/staf/income/file_test_4.txt",
                                  "/test/node/eadcc622739d58e8a78170b67c6ff9f5");
        localFileToArchiveMap.put("src/test/resources/staf/income/file_test_5.txt",
                                  "/test/node/eadcc622739d58e8a78170b67c6ff9f5");
        localFileToArchiveMap.put("src/test/resources/staf/income/file_test_2.txt",
                                  "/test/node/eadcc622739d58e8a78170b67c6ff9f5");
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

        Assert.assertEquals(5, controller.getRawFilesArchived().size());

        // Check thaht STAF Controller return all raw files has archived.
        Set<Path> rawArchivedFiles = controller.getRawFilesArchived();
        Assert.assertEquals(5, rawArchivedFiles.size());
        filesToArchiveWithoutInvalides
                .forEach(file -> Assert.assertTrue("Missing a stored file in STAFController raw files stored",
                                                   rawArchivedFiles.contains(file)));

    }

    @Test
    public void testPrepareTARFilesToStore() throws IOException, STAFException {

        Map<String, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(STAF_TEST_NODE, filesToArchive);

        // Run STAF file preparation
        controller.prepareFilesToArchive(filesToArchivePerNode, STAFArchiveModeEnum.TAR);

        // 3 Tars should have been created created
        Assert.assertEquals("3 Tars should have been created created", 3, controller.getAllFilesToArchive().size());

        // Only 2 ready to send to staf.
        Set<AbstractPhysicalFile> tarToSendToStaf = controller.getAllFilesToArchive().stream()
                .filter(pf -> PhysicalFileStatusEnum.TO_STORE.equals(pf.getStatus())).collect(Collectors.toSet());
        Assert.assertEquals("2 Tars shloud be ready to send to STAF. Last one is not big enougth.", 2,
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

        // Check thaht STAF Controller return all raw files has archived.
        Set<Path> rawArchivedFiles = controller.getRawFilesArchived();
        Assert.assertEquals("The 5 raw files to archive should have need stored in STAF system.", 5,
                            rawArchivedFiles.size());
        filesToArchiveWithoutInvalides
                .forEach(file -> Assert.assertTrue("Missing a stored file in STAFController raw files stored",
                                                   rawArchivedFiles.contains(file)));

    }

    @Test
    public void testPrepareCUTFilesToStore() throws IOException, STAFException {

        Map<String, Set<Path>> filesToArchivePerNode = Maps.newHashMap();
        filesToArchivePerNode.put(STAF_TEST_NODE, filesToArchive);

        controller.prepareFilesToArchive(filesToArchivePerNode, STAFArchiveModeEnum.CUT);
        Assert.assertEquals(20, controller.getAllFilesToArchive().size());

        // All files are ready for transfer
        Set<AbstractPhysicalFile> tarToSendToStaf = controller.getAllFilesToArchive().stream()
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

        // Check thaht the number of files archived in STAF is 5(files)*4(parts per file) = 20(cuted files).
        Assert.assertEquals(20, archivedFiles.size());

        // Check thaht STAF Controller return all raw files has archived.
        Set<Path> rawArchivedFiles = controller.getRawFilesArchived();
        Assert.assertEquals(5, rawArchivedFiles.size());
        filesToArchiveWithoutInvalides
                .forEach(file -> Assert.assertTrue("Missing a stored file in STAFController raw files stored",
                                                   rawArchivedFiles.contains(file)));

    }

}
