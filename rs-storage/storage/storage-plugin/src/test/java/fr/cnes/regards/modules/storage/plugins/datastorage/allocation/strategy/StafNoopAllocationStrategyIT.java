package fr.cnes.regards.modules.storage.plugins.datastorage.allocation.strategy;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.staf.domain.STAFArchive;
import fr.cnes.regards.framework.staf.protocol.STAFURLFactory;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.StafNoopAllocationStrategy;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.staf.STAFDataStorage;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@ContextConfiguration(classes = { MockingResourceServiceConfiguration.class })
@RegardsTransactional
public class StafNoopAllocationStrategyIT extends AbstractRegardsTransactionalIT {

    private static final String STAF_ARCHIVE_NAME = "archive";

    private final static Path WORKSPACE = Paths
            .get(new File("target/AllocationStrategy/STAFNoop/workspace").getAbsolutePath());

    private static final String STAF_NOOP_CONF_LABEL = "STAF_NOOP_CONF_LABEL";

    private static final String LOCAL_STORAGE_LABEL = "DEFAULT_NOOP_CONF_LABEL";

    private static final String STAF_NOOP_ALLOCATION_LABEL = "STAF_NOOP_ALLOCATION_LABEL";

    @Autowired
    private IPluginService pluginService;

    private PluginConfiguration stafDataStorage;

    private PluginConfiguration defaultDataStorage;

    private PluginConfiguration stafNoopAllocationConf;

    private Collection<DataFile> dataFiles;

    private DataFile stafDataFile;

    private DataFile otherDataFile;

    @Before
    public void init() throws ModuleException, MalformedURLException {
        initPlugins();
        initDataFiles();
    }

    private void initDataFiles() throws MalformedURLException {
        dataFiles = Sets.newHashSet();
        AIP aip = getAIP();
        stafDataFile = new DataFile(new URL(STAFURLFactory.STAF_URL_PROTOCOLE, STAF_ARCHIVE_NAME, "truc.json"),
                                    "checksum",
                                    "MD5",
                                    DataType.OTHER,
                                    666L,
                                    MediaType.APPLICATION_JSON,
                                    aip,
                                    "truc");
        dataFiles.add(stafDataFile);
        otherDataFile = new DataFile(new URL("file", "", "local.json"),
                                     "checksum2",
                                     "MD5",
                                     DataType.OTHER,
                                     666L,
                                     MediaType.APPLICATION_JSON,
                                     aip,
                                     "local");
        dataFiles.add(otherDataFile);
    }

    private AIP getAIP() throws MalformedURLException {

        AIPBuilder aipBuilder = new AIPBuilder(new UniformResourceName(OAISIdentifier.AIP,
                                                                       EntityType.DATA,
                                                                       DEFAULT_TENANT,
                                                                       UUID.randomUUID(),
                                                                       1), null, EntityType.DATA);

        String path = System.getProperty("user.dir") + "/src/test/resources/data.txt";
        aipBuilder.getContentInformationBuilder()
                .setDataObject(DataType.RAWDATA, new URL("file", "", path), "MD5", "de89a907d33a9716d11765582102b2e0");
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", "text/plain");
        aipBuilder.addContentInformation();
        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder()
                .addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event", OffsetDateTime.now());

        return aipBuilder.build();
    }

    protected void initPlugins() throws ModuleException, MalformedURLException {
        pluginService.addPluginPackage(IDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(INearlineDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(IOnlineDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(IAllocationStrategy.class.getPackage().getName());
        pluginService.addPluginPackage(STAFDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(LocalDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(StafNoopAllocationStrategy.class.getPackage().getName());
        // lets get a staf data storage conf
        // Init STAF archive parameters for plugin
        STAFArchive archive = new STAFArchive();
        archive.setArchiveName(STAF_ARCHIVE_NAME);
        archive.setPassword("");
        Gson gson = gsonBuilder.create();

        PluginMetaData stafDataStorageMeta = PluginUtils.createPluginMetaData(STAFDataStorage.class,
                                                                              STAFDataStorage.class.getPackage()
                                                                                      .getName(),
                                                                              IDataStorage.class.getPackage()
                                                                                      .getName(),
                                                                              INearlineDataStorage.class.getPackage()
                                                                                      .getName());
        List<PluginParameter> stafDataStorageParams = PluginParametersFactory.build()
                .addParameter("workspaceDirectory", WORKSPACE.toString())
                .addParameter("archiveParameters", gson(archive))
                .addParameter(STAFDataStorage.STAF_STORAGE_TOTAL_SPACE, "9000000000000").getParameters();
        stafDataStorage = new PluginConfiguration(stafDataStorageMeta, STAF_NOOP_CONF_LABEL, stafDataStorageParams);
        stafDataStorage = pluginService.savePluginConfiguration(stafDataStorage);
        // lets get a local data storage conf as default
        List<PluginParameter> defaultParam = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, new URL("file", "", WORKSPACE.toString()).toString())
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, "900000000000").getParameters();
        // new plugin conf for LocalDataStorage storage into target/LocalDataStorageIT
        PluginMetaData defaultMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class,
                                                                      LocalDataStorage.class.getPackage().getName(),
                                                                      IOnlineDataStorage.class.getPackage()
                                                                              .getName(),
                                                                      IDataStorage.class.getPackage().getName());
        defaultDataStorage = new PluginConfiguration(defaultMeta, LOCAL_STORAGE_LABEL, defaultParam);
        defaultDataStorage = pluginService.savePluginConfiguration(defaultDataStorage);
        // lets get a StafNoopAllocationStrategy
        PluginMetaData stafNoopAllocationMeta = PluginUtils.createPluginMetaData(StafNoopAllocationStrategy.class,
                                                                                 StafNoopAllocationStrategy.class
                                                                                         .getPackage().getName(),
                                                                                 IAllocationStrategy.class.getPackage()
                                                                                         .getName());
        List<PluginParameter> stafNoopAllocationParam = PluginParametersFactory.build().addParameter(
                StafNoopAllocationStrategy.DEFAULT_DATA_STORAGE_CONFIGURATION_ID,
                defaultDataStorage.getId().toString()).getParameters();
        stafNoopAllocationConf = new PluginConfiguration(stafNoopAllocationMeta,
                                                         STAF_NOOP_ALLOCATION_LABEL,
                                                         stafNoopAllocationParam);
        stafNoopAllocationConf = pluginService.savePluginConfiguration(stafNoopAllocationConf);
    }

    @Test
    public void testOk() throws ModuleException {
        StafNoopAllocationStrategy stafNoopAllocationStrategy = pluginService.getPlugin(stafNoopAllocationConf.getId());
        Multimap<Long, DataFile> result = stafNoopAllocationStrategy.dispatch(dataFiles);
        Assert.assertTrue(result.containsEntry(stafDataStorage.getId(), stafDataFile));
        Assert.assertTrue(result.containsEntry(defaultDataStorage.getId(), otherDataFile));
    }

}
