package fr.cnes.regards.modules.storage.plugins.staf;

import java.io.IOException;
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeTypeUtils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.staf.STAFArchive;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.OAISIdentifier;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.Event;
import fr.cnes.regards.modules.storage.domain.EventType;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.ProgressManager;
import fr.cnes.regards.modules.storage.plugin.staf.STAFDataStorage;
import fr.cnes.regards.modules.storage.plugin.staf.STAFWorkingSubset;
import fr.cnes.regards.modules.storage.plugin.staf.domain.protocol.STAFURLStreamHandlerFactory;
import fr.cnes.regards.plugins.utils.PluginUtils;

@ContextConfiguration(classes = { STAFDataStorageConfiguration.class, MockingResourceServiceConfiguration.class })
@TestPropertySource(locations = { "classpath:test.properties" })
public class STAFDataStorageTest extends AbstractRegardsServiceIT {

    private static final String STAF_ARCHIVE_NAME = "ARCHIVE_TEST_IT";

    private static final String STAF_ARCHIVE_PASSWORD = "password";

    private static Logger LOG = LoggerFactory.getLogger(STAFDataStorageTest.class);

    private final Set<DataFile> filesToArchive = Sets.newHashSet();

    private final Set<DataFile> filesToArchiveWithoutInvalides = Sets.newHashSet();

    private final Set<DataFile> filesToArchiveMultiplesMode = Sets.newHashSet();

    private final static Path workspace = Paths.get("target/workspace");

    @BeforeClass
    public static void initAll() throws IOException {
        // TODO Add in STAF starter !!!
        URL.setURLStreamHandlerFactory(new STAFURLStreamHandlerFactory());
    }

    @Before
    public void init() throws IOException {

        if (workspace.toFile().exists()) {
            Files.setPosixFilePermissions(workspace,
                                          Sets.newHashSet(PosixFilePermission.OWNER_READ,
                                                          PosixFilePermission.OWNER_WRITE,
                                                          PosixFilePermission.OWNER_EXECUTE));
            FileUtils.deleteDirectory(workspace.toFile());
        }

        AIP aip = new AIP(EntityType.DATA);
        aip.getHistory().add(new Event("testEvent", OffsetDateTime.now(), EventType.SUBMISSION));
        aip.setIpId(new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, "tenant", UUID.randomUUID(), 1)
                .toString());
        filesToArchiveWithoutInvalides.add(new DataFile(
                new URL("file", "",
                        "/home/sbinda/git/rs-storage/storage/storage-plugin/src/test/resources/staf/income/file_test_1.txt"),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip));
        filesToArchiveWithoutInvalides.add(new DataFile(
                new URL("file", "",
                        "/home/sbinda/git/rs-storage/storage/storage-plugin/src/test/resources/staf/income/file_test_2.txt"),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip));
        filesToArchiveWithoutInvalides.add(new DataFile(
                new URL("file", "",
                        "/home/sbinda/git/rs-storage/storage/storage-plugin/src/test/resources/staf/income/file_test_3.txt"),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip));
        filesToArchiveWithoutInvalides.add(new DataFile(
                new URL("file", "",
                        "/home/sbinda/git/rs-storage/storage/storage-plugin/src/test/resources/staf/income/file_test_4.txt"),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip));
        filesToArchiveWithoutInvalides.add(new DataFile(
                new URL("file", "",
                        "/home/sbinda/git/rs-storage/storage/storage-plugin/src/test/resources/staf/income/file_test_5.txt"),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip));
        filesToArchiveWithoutInvalides.add(new DataFile(
                new URL("http", "172.26.47.107", 9020, "/conf/staticConfiguration.js"),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip));

        filesToArchive.addAll(filesToArchiveWithoutInvalides);
        filesToArchive.add(new DataFile(
                new URL("file", "",
                        "/home/sbinda/git/rs-storage/storage/storage-plugin/src/test/resources/staf/income/invalid_test_file.txt"),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip));
        filesToArchive.add(new DataFile(new URL("ftp", "177.7.7.7", "/path/file.txt"),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 3339L, MimeTypeUtils.TEXT_PLAIN, aip));

        filesToArchiveMultiplesMode.addAll(filesToArchive);

        filesToArchiveMultiplesMode.add(new DataFile(
                new URL("file", "",
                        "/home/sbinda/git/rs-storage/storage/storage-plugin/src/test/resources/staf/income/big_file_test_1.txt"),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 29969L, MimeTypeUtils.TEXT_PLAIN, aip));
        filesToArchiveMultiplesMode.add(new DataFile(
                new URL("file", "",
                        "/home/sbinda/git/rs-storage/storage/storage-plugin/src/test/resources/staf/income/big_file_test_2.txt"),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 29969L, MimeTypeUtils.TEXT_PLAIN, aip));
        filesToArchiveMultiplesMode.add(new DataFile(
                new URL("file", "",
                        "/home/sbinda/git/rs-storage/storage/storage-plugin/src/test/resources/staf/income/big_file_test_3.txt"),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 29969L, MimeTypeUtils.TEXT_PLAIN, aip));

        filesToArchiveMultiplesMode.add(new DataFile(
                new URL("file", "",
                        "/home/sbinda/git/rs-storage/storage/storage-plugin/src/test/resources/staf/income/normal_file_test_1.txt"),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 9989L, MimeTypeUtils.TEXT_PLAIN, aip));
        filesToArchiveMultiplesMode.add(new DataFile(
                new URL("file", "",
                        "/home/sbinda/git/rs-storage/storage/storage-plugin/src/test/resources/staf/income/normal_file_test_2.txt"),
                "eadcc622739d58e8a78170b67c6ff9f5", "md5", DataType.RAWDATA, 9989L, MimeTypeUtils.TEXT_PLAIN, aip));
    }

    @Test
    public void storeSingleModeTest() {

        // Add plugin package
        List<String> packages = Lists.newArrayList();
        packages.add("fr.cnes.regards.modules.storage.plugin.staf");

        // Mock the progress manager to verify the number of call for succeed and failted files.
        ProgressManager pm = Mockito.mock(ProgressManager.class);
        Mockito.verify(pm, Mockito.times(0)).storageFailed(Mockito.any(), Mockito.any());
        Mockito.verify(pm, Mockito.times(0)).storageSucceed(Mockito.any(), Mockito.any());

        // Init STAF archive parameters for plugin
        STAFArchive archive = new STAFArchive();
        archive.setArchiveName(STAF_ARCHIVE_NAME);
        archive.setGFAccount(false);
        archive.setPassword(STAF_ARCHIVE_PASSWORD);
        Gson gson = new Gson();

        // Init plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter("workspaceDirectory", "target/workspace")
                .addParameter("archiveParameters", gson.toJson(archive)).getParameters();

        // Get plugin
        STAFDataStorage plugin = PluginUtils.getPlugin(parameters, STAFDataStorage.class, packages, Maps.newHashMap());

        // prepare files
        Set<STAFWorkingSubset> subsets = plugin.prepare(filesToArchive);

        Assert.assertEquals("There should be 1 subset created", 1, subsets.size());

        // Store each subset prepared
        subsets.forEach(subset -> {
            plugin.store(subset, false, pm);
        });

        // One file is on error (file does not exists)
        Mockito.verify(pm, Mockito.times(1)).storageFailed(Mockito.any(), Mockito.any());
        // 6 Files are stored.
        Mockito.verify(pm, Mockito.times(6)).storageSucceed(Mockito.any(), Mockito.any());
    }

    @Test
    public void storeMultipleModesTest() {

        // Add plugin package
        List<String> packages = Lists.newArrayList();
        packages.add("fr.cnes.regards.modules.storage.plugin.staf");

        // Init STAF archive parameters for plugin
        STAFArchive archive = new STAFArchive();
        archive.setArchiveName(STAF_ARCHIVE_NAME);
        archive.setGFAccount(false);
        archive.setPassword(STAF_ARCHIVE_PASSWORD);
        Gson gson = new Gson();

        // Init plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter("workspaceDirectory", "target/workspace")
                .addParameter("archiveParameters", gson.toJson(archive)).getParameters();

        // Get plugin
        STAFDataStorage plugin = PluginUtils.getPlugin(parameters, STAFDataStorage.class, packages, Maps.newHashMap());

        // prepare files
        Set<STAFWorkingSubset> subsets = plugin.prepare(filesToArchiveMultiplesMode);

        Assert.assertEquals("There should be 3 subsets created", 3, subsets.size());

        // Store each subset prepared
        subsets.forEach(subset -> {
            // Mock the progress manager to verify the number of call for succeed and failted files.
            ProgressManager pm = Mockito.mock(ProgressManager.class);
            Mockito.verify(pm, Mockito.times(0)).storageFailed(Mockito.any(), Mockito.any());
            Mockito.verify(pm, Mockito.times(0)).storageSucceed(Mockito.any(), Mockito.any());
            plugin.store(subset, false, pm);

            switch (subset.getMode()) {
                case CUT:
                    // 3 files should have been stored in CUT MODE
                    Mockito.verify(pm, Mockito.times(0)).storageFailed(Mockito.any(), Mockito.any());
                    Mockito.verify(pm, Mockito.times(3)).storageSucceed(Mockito.any(), Mockito.any());
                    break;
                case NORMAL:
                    // 2 files should have been stored in NORMAL MODE
                    Mockito.verify(pm, Mockito.times(0)).storageFailed(Mockito.any(), Mockito.any());
                    Mockito.verify(pm, Mockito.times(2)).storageSucceed(Mockito.any(), Mockito.any());
                    break;
                case TAR:
                    // 6 files should have been stored in TAR MODE. 1 failed.
                    Mockito.verify(pm, Mockito.times(1)).storageFailed(Mockito.any(), Mockito.any());
                    Mockito.verify(pm, Mockito.times(6)).storageSucceed(Mockito.any(), Mockito.any());
                    break;
                default:
                    break;

            }
        });
    }

    @Test
    public void storeTestWorkspaceUnavailable() throws IOException {

        // Create workspace directory and set writes to simulate access denied.
        Path workspace = Paths.get("target/workspace");
        Files.createDirectories(workspace);
        Files.setPosixFilePermissions(workspace, Sets.newHashSet());

        // Mock the progress manager to verify the number of call for succeed and failted files.
        ProgressManager pm = Mockito.mock(ProgressManager.class);
        Mockito.verify(pm, Mockito.times(0)).storageFailed(Mockito.any(), Mockito.any());
        Mockito.verify(pm, Mockito.times(0)).storageSucceed(Mockito.any(), Mockito.any());

        // Add plugin package
        List<String> packages = Lists.newArrayList();
        packages.add("fr.cnes.regards.modules.storage.plugin.staf");

        // Init STAF archive parameters for plugin
        STAFArchive archive = new STAFArchive();
        archive.setArchiveName(STAF_ARCHIVE_NAME);
        archive.setGFAccount(false);
        archive.setPassword(STAF_ARCHIVE_PASSWORD);
        Gson gson = new Gson();

        // Init plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter("workspaceDirectory", "target/workspace")
                .addParameter("archiveParameters", gson.toJson(archive)).getParameters();

        // Get plugin
        STAFDataStorage plugin = PluginUtils.getPlugin(parameters, STAFDataStorage.class, packages, Maps.newHashMap());

        // prepare files
        Set<STAFWorkingSubset> subsets = plugin.prepare(filesToArchive);

        Assert.assertEquals("There should be 1 subset created", 1, subsets.size());

        // Store each subset prepared
        subsets.forEach(subset -> {
            plugin.store(subset, false, pm);
        });

        // All files are on error. Workspace is not accessible.
        Mockito.verify(pm, Mockito.times(7)).storageFailed(Mockito.any(), Mockito.any());
        // 6 Files are stored.
        Mockito.verify(pm, Mockito.times(0)).storageSucceed(Mockito.any(), Mockito.any());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @AfterClass
    public static void postTest() throws IOException {
        if (workspace.toFile().exists()) {
            Files.setPosixFilePermissions(workspace,
                                          Sets.newHashSet(PosixFilePermission.OWNER_READ,
                                                          PosixFilePermission.OWNER_WRITE,
                                                          PosixFilePermission.OWNER_EXECUTE));
        }
    }

}
