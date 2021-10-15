/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.jayway.jsonpath.JsonPath;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import fr.cnes.regards.modules.storage.domain.plugin.StorageType;
import fr.cnes.regards.modules.storage.rest.plugin.SimpleOnlineDataStorage;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.request.FileStorageRequestService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import fr.cnes.regards.modules.storage.service.location.StoragePluginConfigurationHandler;
import io.vavr.collection.List;
import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.RequestMethod;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;

import static org.junit.Assert.*;

/**
 * @author Sébastien Binda
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = {
    "spring.jpa.properties.hibernate.default_schema=storage_rest_it",
    "regards.storage.quota.report.tick=1",
    "regards.amqp.enabled=true"
})
@ActiveProfiles(value = { "testAmqp", "default", "test" }, inheritProfiles = false)
public class FileReferenceControllerIT extends AbstractRegardsTransactionalIT implements IHandler<NotificationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceControllerIT.class);

    private static final String TARGET_STORAGE = "target";

    private static final String STORAGE_PATH = "target/ONLINE-STORAGE";

    @Autowired
    private FileStorageRequestService storeReqService;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private StorageLocationConfigurationService prioritizedDataStorageService;

    @Autowired
    protected StoragePluginConfigurationHandler storagePlgConfHandler;

    @Autowired
    private IGroupRequestInfoRepository reqInfoRepository;

    @Autowired
    protected IFileReferenceRepository fileRepo;

    @Autowired
    private IDownloadQuotaRepository quotaRepository;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    private String storedFileChecksum;

    private final AtomicInteger notificationEvents = new AtomicInteger(0);

    private void clear() throws IOException {
        quotaRepository.deleteAll();
        reqInfoRepository.deleteAll();
        fileRepo.deleteAll();
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
        FileReferenceMetaInfo metaInfo =
            new FileReferenceMetaInfo(
                checksum,
                algorithm,
                filePath.getFileName().toString(),
                null,
                MediaType.APPLICATION_OCTET_STREAM
            );
        metaInfo.setType(DataType.RAWDATA.name());
        tenantResolver.forceTenant(getDefaultTenant());
        storeReqService.handleRequest("rest-test", "source1", "session1", metaInfo,
                                      filePath.toAbsolutePath().toUri().toURL().toString(),
                                      TARGET_STORAGE, Optional.of("/sub/dir/1/"), UUID.randomUUID().toString());
        // Wait for storage file referenced
        boolean found = false;
        int loops = 100;
        do {
            tenantResolver.forceTenant(getDefaultTenant());
            found = fileRefService.search(TARGET_STORAGE, checksum).isPresent();
            Thread.sleep(1_000);
            loops--;
        } while (!found && (loops > 0));
        if (!found) {
            LOGGER.error("Timeout for file reference");
        }
        storedFileChecksum = checksum;
        subscriber.subscribeTo(NotificationEvent.class, this);
    }

    @After
    public void teardown() {
        subscriber.unsubscribeFrom(NotificationEvent.class, true);
        subscriber.purgeQueue(NotificationEvent.class, FileReferenceControllerIT.class);
        notificationEvents.set(0);
    }

    @Test
    public void downloadFileError() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusNotFound();
        performDefaultGet(FileReferenceController.FILE_PATH + FileReferenceController.DOWNLOAD_PATH,
                          requestBuilderCustomizer, "File download response status should be NOT_FOUND.",
                          UUID.randomUUID().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_130")
    @Requirement("REGARDS_DSL_STO_ARC_200")
    @Purpose("Check file download")
    public void downloadFileSuccess() {
        Mono.defer(() -> {
            RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
            performDefaultGet(FileReferenceController.FILE_PATH + FileReferenceController.DOWNLOAD_PATH,
                requestBuilderCustomizer, "File download response status should be OK", storedFileChecksum);
            return Mono.empty();
        })
            // Retry in case of weird but transient "Spring headers" error.
            // Still, let AssertionErrors fail the test downstream.
            .retry(t -> ! (t instanceof AssertionError))
            // blow up maybe?
            .block();
    }

    @Test
    public void download_failed_cause_quota_max_exceeded() {

        tenantResolver.forceTenant(getDefaultTenant());

        String userEmail = UUID.randomUUID().toString();
        long maxQuota = 5L;
        long rateLimit = 10_000L;
        quotaRepository.save(
            new DownloadQuotaLimits(
                getDefaultTenant(),
                userEmail,
                maxQuota,
                rateLimit
            )
        );

        String urlTemplate = FileReferenceController.FILE_PATH + FileReferenceController.DOWNLOAD_PATH;
        String authToken = manageSecurity(getDefaultTenant(), urlTemplate, RequestMethod.GET,
            userEmail, getDefaultRole());

        LongStream.range(0, maxQuota+1)
            .forEach(i -> {
                if (i < maxQuota) {
                    RequestBuilderCustomizer requestBuilderCustomizer =
                        customizer()
                            .expectStatusOk();
                    performGet(urlTemplate, authToken, requestBuilderCustomizer, "File download response status should be OK", storedFileChecksum);
                } else {
                    assertEquals("No notification should be present at this point", 0, notificationEvents.get());
                    RequestBuilderCustomizer requestBuilderCustomizer =
                        customizer()
                            .expectStatus(HttpStatus.TOO_MANY_REQUESTS);
                    performGet(urlTemplate, authToken, requestBuilderCustomizer, "File download response status should be 429", storedFileChecksum);
                    // there's been a notification send for that
                    Try.run(() -> Thread.sleep(5_000)); // wait for batch reporting, at most 1 sec as per this test properties
                    assertEquals("A notification should have been sent on quota exceeded", 1, notificationEvents.get());
                }
            });
    }

    @Test
    public void download_failed_cause_rate_limit_exceeded() {

        tenantResolver.forceTenant(getDefaultTenant());

        String userEmail = UUID.randomUUID().toString();
        quotaRepository.save(
            new DownloadQuotaLimits(
                getDefaultTenant(),
                userEmail,
                10L,
                0L
            )
        );

        String urlTemplate = FileReferenceController.FILE_PATH + FileReferenceController.DOWNLOAD_PATH;
        String authToken = manageSecurity(getDefaultTenant(), urlTemplate, RequestMethod.GET,
            userEmail, getDefaultRole());

        RequestBuilderCustomizer requestBuilderCustomizer =
            customizer()
                .expectStatus(HttpStatus.TOO_MANY_REQUESTS);
        performGet(urlTemplate, authToken, requestBuilderCustomizer, "File download response status should be 429", storedFileChecksum);
        // there's been a notification send for that
        Try.run(() -> Thread.sleep(5_000)); // wait for batch reporting, at most 1 sec as per this test properties
        assertEquals("A notification should have been sent on rate exceeded", 1, notificationEvents.get());
    }

    @Test
    public void rate_limiting_ends_eventually() throws InterruptedException {

        tenantResolver.forceTenant(getDefaultTenant());

        String userEmail = UUID.randomUUID().toString();
        long maxQuota = -1L; // unlimited because IDK how many retries will be needed until the rate limiter gets angry
        long rateLimit = 2L; // low enough in order to increase the chance of hitting the rate limiter
        quotaRepository.save(
            new DownloadQuotaLimits(
                getDefaultTenant(),
                userEmail,
                maxQuota,
                rateLimit
            )
        );

        AtomicReference<List<Integer>> downloadReqStatuses = new AtomicReference<>(List.empty());
        AtomicReference<List<Integer>> currentRatesHistory = new AtomicReference<>(List.empty());

        // try to exceed the rate limit
        int nbDownloads = 1_000;
        int maxConcurrency = Runtime.getRuntime().availableProcessors();

        // latch to know when all the downloads are finished (independently of the nb of retries)
        CountDownLatch latch = new CountDownLatch(nbDownloads);

        // periodically check current rate and store the observed value
        JsonPath jsonPath = JsonPath.compile("$.currentRate");
        Disposable monitor =
            Flux.interval(Duration.ofMillis(50L))
                .concatMap(i ->
                    Mono.defer(() -> {
                        RequestBuilderCustomizer requestBuilderCustomizer =
                            customizer()
                                // let assertion pass, I want to accumulate status codes!
                                .expect(r -> assertTrue(true));
                        String api = DownloadQuotaController.PATH_CURRENT_QUOTA;
                        String authToken = manageSecurity(getDefaultTenant(), api, RequestMethod.GET, userEmail, getDefaultRole());
                        ResultActions res = performGet(api, authToken, requestBuilderCustomizer, "Get current quotas should not blow up");
                        MvcResult result = res.andReturn();

                        try {
                            return Mono.<Integer>just(jsonPath.read(result.getResponse().getContentAsString()));
                        } catch (UnsupportedEncodingException e) {
                            return Mono.error(e);
                        }
                    }).retry()
                )
                // record each currentRate observed
                .subscribe(currentRate -> currentRatesHistory.updateAndGet(l -> l.append(currentRate)));

        // record the max nb of requests sent in parallel, just to be sure that the test was relevant
        // (if max concurrent calls <= rate limit then the test was useless)
        AtomicReference<List<Integer>> maxConcurrentCalls = new AtomicReference<>(List.of(0));

        // try to make each download
        RuntimeException unexpectedResultEx = new RuntimeException("Unexpected result");
        Disposable hammer = Flux.range(0, nbDownloads)
            .flatMap(
                ignored -> Mono.defer(() -> {
                    // increase the nb of concurrent calls
                    maxConcurrentCalls.updateAndGet(l -> l.append(l.last()+1));

                    // download
                    RequestBuilderCustomizer requestBuilderCustomizer =
                        customizer()
                            // let assertion pass, I want to accumulate status codes!
                            .expect(r -> assertTrue(true));
                    String urlTemplate = FileReferenceController.FILE_PATH + FileReferenceController.DOWNLOAD_PATH;
                    String authToken = manageSecurity(getDefaultTenant(), urlTemplate, RequestMethod.GET, userEmail, getDefaultRole());
                    ResultActions res = performGet(urlTemplate, authToken, requestBuilderCustomizer, "File download response status should not blow up at this point", storedFileChecksum);
                    int status = res.andReturn().getResponse().getStatus();

                    // record the download status (200, 429, 500, other ?)
                    downloadReqStatuses.updateAndGet(l -> l.append(status));

                    // call finished, decrease the nb of concurrent calls
                    maxConcurrentCalls.updateAndGet(l -> l.append(l.last()-1));

                    // if the result is not 200 then return and error Mono in order to retry
                    if (status == HttpStatus.OK.value()) {
                        return Mono.just(status);
                    } else {
                        return Mono.error(unexpectedResultEx).delayElement(Duration.ofMillis(200L));
                    }
                })
                    // retry until we finally get the result we expect (200 download successful)
                    .retry(t -> t == unexpectedResultEx)
                    // use the dedicated thread pool
                    .subscribeOn(Schedulers.newParallel("hammer", maxConcurrency))
                ,
                maxConcurrency
            )
            // Spring warm up time
            .delaySubscription(Duration.ofSeconds(15))
            // for each OK result, count down
            .subscribe(ignored -> latch.countDown());

        // wait for the calls to end (max 60secs)
        boolean timely = latch.await(120, TimeUnit.SECONDS);
        // free resources
        hammer.dispose();
        // cancel monitor (hammer has finished) but wait a bit so we can observe the currentRate going down
        Thread.sleep(1_000);
        monitor.dispose();

//        LOGGER.info("concurrent="+maxConcurrentCalls.get().mkString(","));
//        LOGGER.info("downloadReqStatuses="+downloadReqStatuses.get().mkString(","));
//        LOGGER.info("rates="+currentRatesHistory.get().mkString(","));
//        LOGGER.info("reqs count="+downloadReqStatuses.get().size());
        assertTrue(
            "Test should have ended in a timely manner. Check your setup, the delay is either too short or the test took longer than expected (are you on a crowded environment?).",
            timely);
        assertTrue(
            "Test should have sent more concurrent requests than rateLimit, otherwise the whole test would be rather useless. Please check your setup.",
            maxConcurrentCalls.get().reduce(Integer::max) > rateLimit);
        assertTrue(
            "Hitting the rate limiter should have caused HTTP 429 errors and retries of the failed calls until a 200 is eventually returned, hence more calls should have been made than initially requested.",
            nbDownloads < downloadReqStatuses.get().size());
        assertTrue(
            "Observed rate history should never have exceeded the rate limit (in this single node setting at least).",
            currentRatesHistory.get().reduce(Integer::max) <= rateLimit
        );
        assertFalse(
            "Each time the rate went up above 0 it should eventually have gone back to zero.",
            currentRatesHistory.get()
                .foldLeft(false, (aboveZero, next) -> next > 0));
        // there's been many notifications send for that
        assertTrue("Several notification should have been sent on quota exceeded", notificationEvents.get() > 1);
    }

    private void initDataStoragePluginConfiguration() throws ModuleException {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
            Files.createDirectories(Paths.get(STORAGE_PATH));

            Set<IPluginParam> parameters = IPluginParam
                    .set(IPluginParam.build(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                            STORAGE_PATH),
                         IPluginParam.build(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*"),
                         IPluginParam.build(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*"));
            PluginConfiguration dataStorageConf = new PluginConfiguration(TARGET_STORAGE, parameters, 0,
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
