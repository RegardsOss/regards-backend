/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.rest;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storage.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import fr.cnes.regards.modules.storage.domain.plugin.StorageType;
import fr.cnes.regards.modules.storage.rest.plugin.SimpleOnlineDataStorage;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.request.FileStorageRequestService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import fr.cnes.regards.modules.storage.service.location.StoragePluginConfigurationHandler;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

/**
 * @author Sébastien Binda
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_rest_it",
                                   "regards.storage.quota.report.tick=1",
                                   "regards.jpa.multitenant.minPoolSize=3",
                                   "regards.jpa.multitenant.maxPoolSize=3",
                                   "regards.amqp.enabled=true" })
@ActiveProfiles(value = { "testAmqp", "default", "test" }, inheritProfiles = false)
public abstract class AbstractFileReferenceControllerIT extends AbstractRegardsTransactionalIT
    implements IHandler<NotificationEvent> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractFileReferenceControllerIT.class);

    protected static final String TARGET_STORAGE = "target";

    protected static final String STORAGE_PATH = "target/ONLINE-STORAGE";

    @Autowired
    protected FileStorageRequestService storeReqService;

    @Autowired
    protected FileReferenceService fileRefService;

    @Autowired
    protected StorageLocationConfigurationService prioritizedDataStorageService;

    @Autowired
    protected StoragePluginConfigurationHandler storagePlgConfHandler;

    @Autowired
    protected IGroupRequestInfoRepository reqInfoRepository;

    @Autowired
    protected IFileReferenceRepository fileRepo;

    @Autowired
    protected IDownloadQuotaRepository quotaRepository;

    @Autowired
    protected IFileStorageRequestRepository fileStoreReqRepository;

    @Autowired
    protected ISubscriber subscriber;

    @Autowired
    protected IRuntimeTenantResolver tenantResolver;

    protected String storedFileChecksum;

    protected final AtomicInteger notificationEvents = new AtomicInteger(0);

    private void clear() throws IOException {
        quotaRepository.deleteAll();
        reqInfoRepository.deleteAll();
        fileRepo.deleteAll();
        fileStoreReqRepository.deleteAll();
        prioritizedDataStorageService.search(StorageType.ONLINE).forEach(c -> {
            try {
                prioritizedDataStorageService.delete(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
        if (Files.exists(Paths.get("target/storage"))) {
            FileUtils.deleteDirectory(Paths.get(STORAGE_PATH).toFile());
        }
        storagePlgConfHandler.refresh();
        tenantResolver.forceTenant(getDefaultTenant());
    }

    @Before
    public void init()
        throws NoSuchAlgorithmException, FileNotFoundException, IOException, InterruptedException, ModuleException {
        tenantResolver.forceTenant(getDefaultTenant());
        clear();
        initDataStoragePluginConfiguration();
        // Store a file for tests
        Path filePath = Paths.get("src/test/resources/test-file.txt");
        String algorithm = "md5";
        String checksum = ChecksumUtils.computeHexChecksum(new FileInputStream(filePath.toFile()), algorithm);
        FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(checksum,
                                                                   algorithm,
                                                                   filePath.getFileName().toString(),
                                                                   null,
                                                                   MediaType.APPLICATION_OCTET_STREAM);
        metaInfo.setType(DataType.RAWDATA.name());
        tenantResolver.forceTenant(getDefaultTenant());
        storeReqService.handleRequest("rest-test",
                                      "source1",
                                      "session1",
                                      metaInfo,
                                      filePath.toAbsolutePath().toUri().toURL().toString(),
                                      TARGET_STORAGE,
                                      Optional.of("/sub/dir/1/"),
                                      UUID.randomUUID().toString());
        // Wait for storage file referenced
        try {
            Awaitility.await().atMost(100, TimeUnit.SECONDS).until(() -> {
                tenantResolver.forceTenant(getDefaultTenant());
                return fileRefService.search(TARGET_STORAGE, checksum).isPresent();
            });
        } catch (ConditionTimeoutException e) {
            fail("Timeout for file reference");
        }

        storedFileChecksum = checksum;
        subscriber.subscribeTo(NotificationEvent.class, this);
    }

    @After
    public void teardown() {
        subscriber.unsubscribeFrom(NotificationEvent.class, true);
        subscriber.purgeQueue(NotificationEvent.class, AbstractFileReferenceControllerIT.class);
        notificationEvents.set(0);
    }

    protected void initDataStoragePluginConfiguration() throws ModuleException {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
            Files.createDirectories(Paths.get(STORAGE_PATH));

            Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                                                               STORAGE_PATH),
                                                            IPluginParam.build(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN,
                                                                               "error.*"),
                                                            IPluginParam.build(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN,
                                                                               "delErr.*"));
            PluginConfiguration dataStorageConf = new PluginConfiguration(TARGET_STORAGE,
                                                                          parameters,
                                                                          0,
                                                                          dataStoMeta.getPluginId());
            prioritizedDataStorageService.create(TARGET_STORAGE, dataStorageConf, 1_000_000L);
            storagePlgConfHandler.refresh();
            tenantResolver.forceTenant(getDefaultTenant());
        } catch (IOException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    @Override
    public void handle(String tenant, NotificationEvent notificationEvent) {
        notificationEvents.incrementAndGet();
    }
}
