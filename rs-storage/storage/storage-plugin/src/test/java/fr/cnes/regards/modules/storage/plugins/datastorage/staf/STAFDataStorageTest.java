/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugins.datastorage.staf;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeTypeUtils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.staf.domain.STAFArchive;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.IProgressManager;
import fr.cnes.regards.modules.storage.plugin.datastorage.staf.STAFDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.staf.STAFStoreWorkingSubset;
import fr.cnes.regards.modules.storage.plugin.datastorage.staf.STAFWorkingSubset;

@ContextConfiguration(classes = { STAFDataStorageConfiguration.class, MockingResourceServiceConfiguration.class })
@TestPropertySource(locations = { "classpath:test.properties" })
public class STAFDataStorageTest extends AbstractRegardsServiceIT {

    private static final String STAF_ARCHIVE_NAME = "ARCHIVE_TEST_IT";

    private static final String STAF_ARCHIVE_PASSWORD = "password";

    private static Logger LOG = LoggerFactory.getLogger(STAFDataStorageTest.class);

    private final Set<StorageDataFile> filesToArchive = Sets.newHashSet();

    private final Set<StorageDataFile> filesToArchiveWithoutInvalides = Sets.newHashSet();

    private final Set<StorageDataFile> filesToArchiveMultiplesMode = Sets.newHashSet();

    private final static Path WORKSPACE = Paths.get(new File("target/STAF/workspace").getAbsolutePath());

    private final static Path RESTORATION_PATH = Paths.get(new File("target/STAF/restsore").getAbsolutePath());

    private static String incomTestSourcesDir = new File("src/test/resources/staf/income/file_test_1.txt")
            .getAbsoluteFile().getParent();

    private static String STAF_NODE = "/STAF/NODE/TEST";

    @Before
    public void init() throws IOException {

        if (WORKSPACE.toFile().exists()) {
            Files.setPosixFilePermissions(WORKSPACE,
                                          Sets.newHashSet(PosixFilePermission.OWNER_READ,
                                                          PosixFilePermission.OWNER_WRITE,
                                                          PosixFilePermission.OWNER_EXECUTE));
            FileUtils.deleteDirectory(WORKSPACE.toFile());
        }

        if (RESTORATION_PATH.toFile().exists()) {
            Files.setPosixFilePermissions(RESTORATION_PATH,
                                          Sets.newHashSet(PosixFilePermission.OWNER_READ,
                                                          PosixFilePermission.OWNER_WRITE,
                                                          PosixFilePermission.OWNER_EXECUTE));
            FileUtils.deleteDirectory(RESTORATION_PATH.toFile());
        }
        Files.createDirectories(RESTORATION_PATH);

        AIPBuilder builder = new AIPBuilder(
                new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, "tenant", UUID.randomUUID(), 1), null,
                EntityType.DATA);
        builder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "testEvent",
                                                              OffsetDateTime.now());
        AIP aip = builder.build();

        // lets first make some file that should be stored in TAR mode
        filesToArchiveWithoutInvalides
                .add(new StorageDataFile(Sets.newHashSet(new URL("file", "", incomTestSourcesDir + "/file_test_1.txt")),
                        "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN,
                        aip, "file_test_1.txt", STAF_NODE));
        filesToArchiveWithoutInvalides
                .add(new StorageDataFile(Sets.newHashSet(new URL("file", "", incomTestSourcesDir + "/file_test_2.txt")),
                        "8e3d5e32119c70881316a1a2b17a64d1", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN,
                        aip, "file_test_2.txt", STAF_NODE));
        filesToArchiveWithoutInvalides
                .add(new StorageDataFile(Sets.newHashSet(new URL("file", "", incomTestSourcesDir + "/file_test_3.txt")),
                        "1f4add9aecfc4c623cdda55771f4b984", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN,
                        aip, "file_test_3.txt", STAF_NODE));
        filesToArchiveWithoutInvalides
                .add(new StorageDataFile(Sets.newHashSet(new URL("file", "", incomTestSourcesDir + "/file_test_4.txt")),
                        "955fd5652aadd97329a50e029163f3a9", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN,
                        aip, "file_test_4.txt", STAF_NODE));
        filesToArchiveWithoutInvalides
                .add(new StorageDataFile(Sets.newHashSet(new URL("file", "", incomTestSourcesDir + "/file_test_5.txt")),
                        "61142380c96f899eaea71b229dcc4247", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN,
                        aip, "file_test_5.txt", STAF_NODE));

        filesToArchive.addAll(filesToArchiveWithoutInvalides);

        //lets add some files that won't be stored because they are considered as not existing
        filesToArchive.add(new StorageDataFile(
                Sets.newHashSet(new URL("http", "172.26.47.52", 80, "/conf/staticConfiguration.js")),
                "eadcc622739d58e8a78170b67c6ff9f3", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                "staticConfiguration.js", STAF_NODE));
        filesToArchive.add(new StorageDataFile(
                Sets.newHashSet(new URL("file", "", incomTestSourcesDir + "/invalid_test_file.txt")),
                "eadcc622739d58e8a78170b67c6ff9f2", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                "invalid_test_file.txt", STAF_NODE));
        filesToArchive.add(new StorageDataFile(Sets.newHashSet(new URL("ftp", "177.7.7.7", "/path/file.txt")),
                "eadcc622739d58e8a78170b67c6ff9f1", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                "file.txt", STAF_NODE));

        filesToArchiveMultiplesMode.addAll(filesToArchive);

        // lets add some big files that will be stored in CUT mode
        filesToArchiveMultiplesMode.add(new StorageDataFile(
                Sets.newHashSet(new URL("file", "", incomTestSourcesDir + "/big_file_test_1.txt")),
                "eadcc622739d58e8a78170b67c6ff9f0", "md5", DataType.RAWDATA, 29969L, MimeTypeUtils.TEXT_PLAIN, aip,
                "big_file_test_1.txt", STAF_NODE));
        filesToArchiveMultiplesMode.add(new StorageDataFile(
                Sets.newHashSet(new URL("file", "", incomTestSourcesDir + "/big_file_test_2.txt")),
                "eadcc622739d58e8a78170b67c6ff9f7", "md5", DataType.RAWDATA, 29969L, MimeTypeUtils.TEXT_PLAIN, aip,
                "big_file_test_2.txt", STAF_NODE));
        filesToArchiveMultiplesMode.add(new StorageDataFile(
                Sets.newHashSet(new URL("file", "", incomTestSourcesDir + "/big_file_test_3.txt")),
                "eadcc622739d58e8a78170b67c6ff9f8", "md5", DataType.RAWDATA, 29969L, MimeTypeUtils.TEXT_PLAIN, aip,
                "big_file_test_3.txt", STAF_NODE));

        //lets add some file that will be stored in NORMAL mode
        filesToArchiveMultiplesMode.add(new StorageDataFile(
                Sets.newHashSet(new URL("file", "", incomTestSourcesDir + "/normal_file_test_1.txt")),
                "eadcc622739d58e8a78170b67c6ff9f9", "md5", DataType.RAWDATA, 9989L, MimeTypeUtils.TEXT_PLAIN, aip,
                "normal_file_test_1.txt", STAF_NODE));
        filesToArchiveMultiplesMode.add(new StorageDataFile(
                Sets.newHashSet(new URL("file", "", incomTestSourcesDir + "/normal_file_test_2.txt")),
                "eadcc622739d58e8a78170b67c6ff9g4", "md5", DataType.RAWDATA, 9989L, MimeTypeUtils.TEXT_PLAIN, aip,
                "normal_file_test_2.txt", STAF_NODE));
    }

    /**
     * Store files in the 3 existing modes TAR, CUT and NORMAL.
     * <ul>
     * <li>11 files available for storage</li>
     * <li>3 files should be stored in CUT mode</li>
     * <li>2 files should be stored in NORMAL mode</li>
     * <li>6 files should be stored in TAR mode</li>
     * <li>2 files unavaiable for storage</li>
     * </ul>
     */
    @Purpose("Test STAF Nearline DataStorage Plugin to store files into STAF system")
    @Requirement("REGARDS_DSL_STO_PLG_220")
    @Test
    public void storeMultipleModesTest() {

        // Add plugin package
        List<String> packages = Lists.newArrayList();
        packages.add("fr.cnes.regards.modules.storage.plugin.datastorage.staf");

        // Init STAF archive parameters for plugin
        STAFArchive archive = new STAFArchive();
        archive.setArchiveName(STAF_ARCHIVE_NAME);
        archive.setPassword(STAF_ARCHIVE_PASSWORD);

        // Init plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter("workspaceDirectory", WORKSPACE.toString()).addParameter("archiveParameters", archive)
                .addParameter(STAFDataStorage.STAF_STORAGE_TOTAL_SPACE, 9000000000000L).getParameters();

        // Get plugin
        STAFDataStorage plugin = PluginUtils.getPlugin(parameters, STAFDataStorage.class, packages, Maps.newHashMap());

        // prepare files
        Set<STAFWorkingSubset> subsets = plugin.prepare(filesToArchiveMultiplesMode,
                                                        DataStorageAccessModeEnum.STORE_MODE);

        Assert.assertEquals("There should be 1 subset created", 1, subsets.size());

        // Store each subset prepared
        subsets.forEach(subset -> {
            STAFStoreWorkingSubset ws = (STAFStoreWorkingSubset) subset;
            Assert.assertEquals(ws.getStafNode().toString(), STAF_NODE);
            // Mock the progress manager to verify the number of call for succeed and failted files.
            IProgressManager pm = Mockito.mock(IProgressManager.class);
            Mockito.verify(pm, Mockito.times(0)).storageFailed(Mockito.any(), Mockito.any());
            Mockito.verify(pm, Mockito.times(0)).storageSucceed(Mockito.any(), Mockito.any(), Mockito.any());
            plugin.store(subset, false, pm);
            // 3 files should have been stored in CUT MODE
            // 2 files should have been stored in NORMAL MODE
            // 5 files should have been stored in TAR MODE.
            // 3 files should not been stored. files does not exists
            Mockito.verify(pm, Mockito.times(3)).storageFailed(Mockito.any(), Mockito.any());
            Mockito.verify(pm, Mockito.times(10)).storageSucceed(Mockito.any(), Mockito.any(), Mockito.any());
        });

    }

    /**
     * Try to store files but the STAF workspace is not available.
     * @throws IOException
     */
    @Test
    public void storeTestWorkspaceUnavailable() throws IOException {

        // Create workspace directory and set writes to simulate access denied.
        Files.createDirectories(WORKSPACE);
        Files.setPosixFilePermissions(WORKSPACE, Sets.newHashSet());

        // Mock the progress manager to verify the number of call for succeed and failted files.
        IProgressManager pm = Mockito.mock(IProgressManager.class);
        Mockito.verify(pm, Mockito.times(0)).storageFailed(Mockito.any(), Mockito.any());
        Mockito.verify(pm, Mockito.times(0)).storageSucceed(Mockito.any(), Mockito.any(), Mockito.any());

        // Add plugin package
        List<String> packages = Lists.newArrayList();
        packages.add("fr.cnes.regards.modules.storage.plugin.datastorage.staf");

        // Init STAF archive parameters for plugin
        STAFArchive archive = new STAFArchive();
        archive.setArchiveName(STAF_ARCHIVE_NAME);
        archive.setPassword(STAF_ARCHIVE_PASSWORD);

        // Init plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter("workspaceDirectory", WORKSPACE.toString()).addParameter("archiveParameters", archive)
                .addParameter(STAFDataStorage.STAF_STORAGE_TOTAL_SPACE, 9000000000000L).getParameters();

        // Get plugin
        STAFDataStorage plugin = PluginUtils.getPlugin(parameters, STAFDataStorage.class, packages, Maps.newHashMap());

        // prepare files
        Set<STAFWorkingSubset> subsets = plugin.prepare(filesToArchive, DataStorageAccessModeEnum.STORE_MODE);

        Assert.assertEquals("There should be 1 subset created", 1, subsets.size());

        // Store each subset prepared
        subsets.forEach(subset -> {
            plugin.store(subset, false, pm);
        });

        // All files are on error. Workspace is not accessible.
        Mockito.verify(pm, Mockito.times(8)).storageFailed(Mockito.any(), Mockito.any());
        // 0 Files are stored.
        Mockito.verify(pm, Mockito.times(0)).storageSucceed(Mockito.any(), Mockito.any(), Mockito.any());
    }

    /**
     * Restore files stored in STAF in the 3 Archiving mode TAR, CUT and NORMAL.
     * @throws MalformedURLException
     */
    @Purpose("Test STAF Nearline DataStorage Plugin to restore files from STAF system")
    @Requirement("REGARDS_DSL_STO_PLG_220")
    @Test
    public void restore() throws MalformedURLException {

        // Mock the progress manager to verify the number of call for succeed and failted files.
        IProgressManager pm = Mockito.mock(IProgressManager.class);
        Mockito.verify(pm, Mockito.times(0)).restoreSucceed(Mockito.any(), Mockito.any());
        Mockito.verify(pm, Mockito.times(0)).restoreFailed(Mockito.any(), Mockito.any());

        // Add plugin package
        List<String> packages = Lists.newArrayList();
        packages.add("fr.cnes.regards.modules.storage.plugin.datastorage.staf");

        // Init STAF archive parameters for plugin
        STAFArchive archive = new STAFArchive();
        archive.setArchiveName(STAF_ARCHIVE_NAME);
        archive.setPassword(STAF_ARCHIVE_PASSWORD);

        // Init plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter("workspaceDirectory", WORKSPACE.toString()).addParameter("archiveParameters", archive)
                .addParameter(STAFDataStorage.STAF_STORAGE_TOTAL_SPACE, 9000000000000L).getParameters();

        // Init Files to restore
        String fileName = "file.txt";
        String cutFileName = "cut_file.txt";
        String tarFileName = "file2.txt";
        Set<StorageDataFile> dataFilesToRestore = Sets.newHashSet();

        AIPBuilder builder = new AIPBuilder(
                new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, "tenant", UUID.randomUUID(), 1), null,
                EntityType.DATA);
        builder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "testEvent",
                                                              OffsetDateTime.now());
        AIP aip = builder.build();

        dataFilesToRestore.add(new StorageDataFile(
                Sets.newHashSet(new URL("staf://" + STAF_ARCHIVE_NAME + "/test/restore/node/" + fileName)),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                fileName, STAF_NODE));
        dataFilesToRestore.add(new StorageDataFile(
                Sets.newHashSet(new URL(
                        "staf://" + STAF_ARCHIVE_NAME + "/test/restore/node/file.tar?filename=" + tarFileName)),
                "eadcc622739d58e8a78170b67c6ff9f6", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                tarFileName, STAF_NODE));
        dataFilesToRestore.add(new StorageDataFile(
                Sets.newHashSet(new URL(
                        "staf://" + STAF_ARCHIVE_NAME + "/test/restore/node/" + cutFileName + "?parts=12")),
                "eadcc622739d58e8a78170b67c6ff9f7", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                cutFileName, STAF_NODE));

        // Get plugin
        STAFDataStorage plugin = PluginUtils.getPlugin(parameters, STAFDataStorage.class, packages, Maps.newHashMap());

        // prepare files
        Set<STAFWorkingSubset> subsets = plugin.prepare(dataFilesToRestore, DataStorageAccessModeEnum.RETRIEVE_MODE);

        Assert.assertEquals("There should be 1 subset created", 1, subsets.size());

        // Store each subset prepared
        subsets.forEach(subset -> {
            plugin.retrieve(subset, RESTORATION_PATH, pm);
        });

        // No restoration error
        Mockito.verify(pm, Mockito.times(0)).restoreFailed(Mockito.any(), Mockito.any());
        // 1 Datafile restored
        Mockito.verify(pm, Mockito.times(3)).restoreSucceed(Mockito.any(), Mockito.any());

        // Check files are really restored.
        Assert.assertTrue("The file should exists after restoration",
                          Paths.get(RESTORATION_PATH.toString(), fileName).toFile().exists());
        Assert.assertTrue("The file should exists after restoration",
                          Paths.get(RESTORATION_PATH.toString(), cutFileName).toFile().exists());
        Assert.assertTrue("The file should exists after restoration",
                          Paths.get(RESTORATION_PATH.toString(), tarFileName).toFile().exists());

    }

    /**
     * Restore a file from a TAR in STAF but file doesn't exists in the TAR.
     * @throws MalformedURLException
     */
    @Test
    public void restoreTARError() throws MalformedURLException {

        // Mock the progress manager to verify the number of call for succeed and failted files.
        IProgressManager pm = Mockito.mock(IProgressManager.class);
        Mockito.verify(pm, Mockito.times(0)).restoreSucceed(Mockito.any(), Mockito.any());
        Mockito.verify(pm, Mockito.times(0)).restoreFailed(Mockito.any(), Mockito.any());

        // Add plugin package
        List<String> packages = Lists.newArrayList();
        packages.add("fr.cnes.regards.modules.storage.plugin.datastorage.staf");

        // Init STAF archive parameters for plugin
        STAFArchive archive = new STAFArchive();
        archive.setArchiveName(STAF_ARCHIVE_NAME);
        archive.setPassword(STAF_ARCHIVE_PASSWORD);

        // Init plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter("workspaceDirectory", WORKSPACE.toString()).addParameter("archiveParameters", archive)
                .addParameter(STAFDataStorage.STAF_STORAGE_TOTAL_SPACE, 9000000000000L).getParameters();

        // Init Files to restore
        String fileName = "file.txt";
        String cutFileName = "cut_file.txt";
        // Test to retrieve a file from a tar that does not exists in the TAR. (see file.tar in
        // src/test/resources/staf/mock
        String tarFileName = "fileNotInTar.txt";
        Set<StorageDataFile> dataFilesToRestore = Sets.newHashSet();

        AIPBuilder builder = new AIPBuilder(
                new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, "tenant", UUID.randomUUID(), 1), null,
                EntityType.DATA);
        builder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "testEvent",
                                                              OffsetDateTime.now());
        AIP aip = builder.build();

        dataFilesToRestore.add(new StorageDataFile(
                Sets.newHashSet(new URL("staf://" + STAF_ARCHIVE_NAME + "/test/restore/node/" + fileName)),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                fileName, STAF_NODE));
        dataFilesToRestore.add(new StorageDataFile(
                Sets.newHashSet(new URL(
                        "staf://" + STAF_ARCHIVE_NAME + "/test/restore/node/file.tar?filename=" + tarFileName)),
                "eadcc622739d58e8a78170b67c6ff9f6", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                tarFileName, STAF_NODE));
        dataFilesToRestore.add(new StorageDataFile(
                Sets.newHashSet(new URL(
                        "staf://" + STAF_ARCHIVE_NAME + "/test/restore/node/" + cutFileName + "?parts=12")),
                "eadcc622739d58e8a78170b67c6ff9f7", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                cutFileName, STAF_NODE));

        // Get plugin
        STAFDataStorage plugin = PluginUtils.getPlugin(parameters, STAFDataStorage.class, packages, Maps.newHashMap());

        // prepare files
        Set<STAFWorkingSubset> subsets = plugin.prepare(dataFilesToRestore, DataStorageAccessModeEnum.RETRIEVE_MODE);

        Assert.assertEquals("There should be 1 subset created", 1, subsets.size());

        // Store each subset prepared
        subsets.forEach(subset -> {
            plugin.retrieve(subset, RESTORATION_PATH, pm);
        });

        // No restoration error
        Mockito.verify(pm, Mockito.times(1)).restoreFailed(Mockito.any(), Mockito.any());
        // 1 Datafile restored
        Mockito.verify(pm, Mockito.times(2)).restoreSucceed(Mockito.any(), Mockito.any());

        // Check files are really restored.
        Assert.assertTrue("The file should exists after restoration",
                          Paths.get(RESTORATION_PATH.toString(), fileName).toFile().exists());
        Assert.assertTrue("The file should exists after restoration",
                          Paths.get(RESTORATION_PATH.toString(), cutFileName).toFile().exists());
        Assert.assertFalse("The file should not exists after restoration",
                           Paths.get(RESTORATION_PATH.toString(), tarFileName).toFile().exists());

    }

    /**
     * Try to restore non existing file from STAF.
     * @throws MalformedURLException
     */
    @Test
    public void restoreFileNotFoundError() throws MalformedURLException {

        // Mock the progress manager to verify the number of call for succeed and failted files.
        IProgressManager pm = Mockito.mock(IProgressManager.class);
        Mockito.verify(pm, Mockito.times(0)).restoreSucceed(Mockito.any(), Mockito.any());
        Mockito.verify(pm, Mockito.times(0)).restoreFailed(Mockito.any(), Mockito.any());

        // Add plugin package
        List<String> packages = Lists.newArrayList();
        packages.add("fr.cnes.regards.modules.storage.plugin.datastorage.staf");

        // Init STAF archive parameters for plugin
        STAFArchive archive = new STAFArchive();
        archive.setArchiveName(STAF_ARCHIVE_NAME);
        archive.setPassword(STAF_ARCHIVE_PASSWORD);

        // Init plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter("workspaceDirectory", WORKSPACE.toString()).addParameter("archiveParameters", archive)
                .addParameter(STAFDataStorage.STAF_STORAGE_TOTAL_SPACE, 9000000000000L).getParameters();

        // Init Files to restore
        // Add error in the file name simulate a restoration error in the restorationMock from
        // STAFDataStorageConfiguration
        String fileName = "error.txt";
        String cutFileName = "cut_file.txt";
        String tarFileName = "fileNotInTar.txt";
        Set<StorageDataFile> dataFilesToRestore = Sets.newHashSet();

        AIPBuilder builder = new AIPBuilder(
                new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, "tenant", UUID.randomUUID(), 1), null,
                EntityType.DATA);
        builder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "testEvent",
                                                              OffsetDateTime.now());
        AIP aip = builder.build();

        dataFilesToRestore.add(new StorageDataFile(
                Sets.newHashSet(new URL("staf://" + STAF_ARCHIVE_NAME + "/test/restore/node/" + fileName)),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                fileName, STAF_NODE));
        dataFilesToRestore.add(new StorageDataFile(
                Sets.newHashSet(new URL(
                        "staf://" + STAF_ARCHIVE_NAME + "/test/restore/node/file.tar?filename=" + tarFileName)),
                "eadcc622739d58e8a78170b67c6ff9f6", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                tarFileName, STAF_NODE));
        dataFilesToRestore.add(new StorageDataFile(
                Sets.newHashSet(new URL(
                        "staf://" + STAF_ARCHIVE_NAME + "/test/restore/node/" + cutFileName + "?parts=12")),
                "eadcc622739d58e8a78170b67c6ff9f7", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                cutFileName, STAF_NODE));

        // Get plugin
        STAFDataStorage plugin = PluginUtils.getPlugin(parameters, STAFDataStorage.class, packages, Maps.newHashMap());

        // prepare files
        Set<STAFWorkingSubset> subsets = plugin.prepare(dataFilesToRestore, DataStorageAccessModeEnum.RETRIEVE_MODE);

        Assert.assertEquals("There should be 1 subset created", 1, subsets.size());

        // Store each subset prepared
        subsets.forEach(subset -> {
            plugin.retrieve(subset, RESTORATION_PATH, pm);
        });

        // All files retore error
        Mockito.verify(pm, Mockito.times(3)).restoreFailed(Mockito.any(), Mockito.any());
        // No Datafile restored
        Mockito.verify(pm, Mockito.times(0)).restoreSucceed(Mockito.any(), Mockito.any());

        // Check files are really restored.
        Assert.assertFalse("The file should exists after restoration",
                           Paths.get(RESTORATION_PATH.toString(), fileName).toFile().exists());
        Assert.assertFalse("The file should exists after restoration",
                           Paths.get(RESTORATION_PATH.toString(), cutFileName).toFile().exists());
        Assert.assertFalse("The file should not exists after restoration",
                           Paths.get(RESTORATION_PATH.toString(), tarFileName).toFile().exists());
    }

    /**
     * Restore files stored in STAF in the 3 Archiving mode TAR, CUT and NORMAL.
     * @throws MalformedURLException
     */
    @Test
    public void delete() throws MalformedURLException {

        // Mock the progress manager to verify the number of call for succeed and failted files.
        IProgressManager pm = Mockito.mock(IProgressManager.class);
        Mockito.verify(pm, Mockito.times(0)).deletionFailed(Mockito.any(), Mockito.any());
        Mockito.verify(pm, Mockito.times(0)).deletionSucceed(Mockito.any());

        // Add plugin package
        List<String> packages = Lists.newArrayList();
        packages.add("fr.cnes.regards.modules.storage.plugin.datastorage.staf");

        // Init STAF archive parameters for plugin
        STAFArchive archive = new STAFArchive();
        archive.setArchiveName(STAF_ARCHIVE_NAME);
        archive.setPassword(STAF_ARCHIVE_PASSWORD);

        // Init plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter("workspaceDirectory", WORKSPACE.toString()).addParameter("archiveParameters", archive)
                .addParameter(STAFDataStorage.STAF_STORAGE_TOTAL_SPACE, 9000000000000L).getParameters();

        // Init Files to restore
        String fileName = "file.txt";
        String cutFileName = "cut_file.txt";
        String tarFileName = "file2.txt";
        Set<StorageDataFile> dataFilesToDelete = Sets.newHashSet();

        AIPBuilder builder = new AIPBuilder(
                new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, "tenant", UUID.randomUUID(), 1), null,
                EntityType.DATA);
        builder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "testEvent",
                                                              OffsetDateTime.now());
        AIP aip = builder.build();

        dataFilesToDelete.add(new StorageDataFile(
                Sets.newHashSet(new URL("staf://" + STAF_ARCHIVE_NAME + "/test/restore/node/" + fileName)),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                fileName, STAF_NODE));
        dataFilesToDelete.add(new StorageDataFile(
                Sets.newHashSet(new URL(
                        "staf://" + STAF_ARCHIVE_NAME + "/test/restore/node/file.tar?filename=" + tarFileName)),
                "eadcc622739d58e8a78170b67c6ff9f6", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                tarFileName, STAF_NODE));
        dataFilesToDelete.add(new StorageDataFile(
                Sets.newHashSet(new URL(
                        "staf://" + STAF_ARCHIVE_NAME + "/test/restore/node/" + cutFileName + "?parts=12")),
                "eadcc622739d58e8a78170b67c6ff9f7", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip,
                cutFileName, STAF_NODE));

        // Get plugin
        STAFDataStorage plugin = PluginUtils.getPlugin(parameters, STAFDataStorage.class, packages, Maps.newHashMap());

        // Delete files
        plugin.delete(new STAFWorkingSubset(dataFilesToDelete), pm);

        // No restoration error
        Mockito.verify(pm, Mockito.times(0)).deletionFailed(Mockito.any(), Mockito.any());
        // 1 Datafile restored
        Mockito.verify(pm, Mockito.times(3)).deletionSucceed(Mockito.any());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @AfterClass
    public static void postTest() throws IOException {
        if (WORKSPACE.toFile().exists()) {
            Files.setPosixFilePermissions(WORKSPACE,
                                          Sets.newHashSet(PosixFilePermission.OWNER_READ,
                                                          PosixFilePermission.OWNER_WRITE,
                                                          PosixFilePermission.OWNER_EXECUTE));
        }
    }

}
