package fr.cnes.regards.modules.storage.service.file.download;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserQuotaAggregate;
import fr.cnes.regards.modules.storage.domain.database.UserRateAggregate;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import fr.cnes.regards.modules.storage.service.file.flow.AvailabilityUpdateCustomTestAction;
import io.vavr.Tuple2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

@TestPropertySource(
    properties = {
        "spring.jpa.properties.hibernate.default_schema=storage_download_quota_tests",
        "regards.storage.cache.path=unused but required" // ¯\_(ツ)_/¯
    }
)
public class QuotaManagerImplIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaManagerImplIT.class);

    @Value("${regards.storage.rate.expiration.tick}") private long rateExpirationTick;

    @Value("${regards.storage.quota.sync.tick}") private long syncTick;

    @Autowired private IDownloadQuotaRepository quotaRepository;

    private IDownloadQuotaRepository quotaRepositoryDelegate;

    @Autowired private ITenantResolver tenantResolver;

    @Autowired private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired @InjectMocks
    private QuotaManagerImpl quotaManager;

    @Autowired
    private IDynamicTenantSettingService dynamicTenantSettingService;

    @Before
    public void setUp() throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        MockitoAnnotations.initMocks(this);
        quotaRepositoryDelegate = quotaRepository;
        quotaRepository =
            Mockito.mock(IDownloadQuotaRepository.class, AdditionalAnswers.delegatesTo(quotaRepository));
        ReflectionTestUtils.setField(quotaManager, "quotaRepository", quotaRepository);

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        quotaManager.setUserDiffsByTenant(new HashMap<>());//HashMap.empty());
        quotaManager.setDiffsAccumulatorByTenant(new HashMap<>());//HashMap.empty());
        // we override cache setting values for tests
        dynamicTenantSettingService.update(StorageSetting.CACHE_PATH_NAME, Paths.get("target", "cache", getDefaultTenant()));
    }

    @After
    public void tearDown() {
        quotaRepository.deleteAll();
        Mockito.clearInvocations(quotaRepository);
    }

    @Test
    public void test_gauges() throws InterruptedException {
        // given
        // there is a user with some quota definition
        DownloadQuotaLimits downloadQuota = new DownloadQuotaLimits(getDefaultTenant(), "foo@bar.com", -1L, -1L);
        Cache<String, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        quotaManager.setUserDiffsByTenant(new HashMap<String, Cache<String, QuotaManagerImpl.UserDiffs>>(){{
            put(getDefaultTenant(), cache);
        }});

        // when
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        // a bunch of threads try to get, increment and decrement its gauge
        int nbRequests = 10_000;
        CountDownLatch latch = new CountDownLatch(nbRequests);
        for (int i = 0; i<nbRequests; i++) {
            CompletableFuture.runAsync(
                () -> {
                    try {
                        runtimeTenantResolver.forceTenant(downloadQuota.getTenant());
                        quotaManager.get(downloadQuota);
                        quotaManager.increment(downloadQuota);
                        quotaManager.decrement(downloadQuota);
                        latch.countDown();
                    } catch (Exception ignored) {}
                },
                executor
            );
        }
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();

        // then
        // the quota is to the rooftop
        assertEquals(nbRequests, quotaManager.get(downloadQuota)._1.getCounter().intValue());
        // the rate is back to zero
        assertEquals(0L, quotaManager.get(downloadQuota)._2.getGauge().longValue());
    }

    @Test
    public void flushSyncAndRefreshQuotas_should_update_instances_quotas_counters_and_return_sum_across_instances() {
        Random rand = new Random();

        // given
        // a user,
        String email = "foo@bar.com";

        // two instances,
        String instanceId1 = UUID.randomUUID().toString();
        String instanceId2 = UUID.randomUUID().toString();

        long quotaOnInstance1 = rand.nextInt(10_000);
        long quotaOnInstance2 = rand.nextInt(10_000);
        quotaRepository.upsertOrCombineDownloadQuota(instanceId1, email, quotaOnInstance1);
        quotaRepository.upsertOrCombineDownloadQuota(instanceId2, email, quotaOnInstance2);

        // and a diff on one of the two instances
        DownloadQuotaLimits downloadQuotaStub = new DownloadQuotaLimits(getDefaultTenant(), email, (long) rand.nextInt(10_000), (long) rand.nextInt(10_000));
        long quotaDiffOnInstance1 = rand.nextInt(10_000);
        Map<String, QuotaManagerImpl.DiffSync> diffSyncs =
            new HashMap<String, QuotaManagerImpl.DiffSync>() {{
                put(email, new QuotaManagerImpl.DiffSync(0L, quotaDiffOnInstance1));
            }};

        // when
        // this instance flushes its diff and refreshes the global gauge value across instances
        quotaManager.flushSyncAndRefreshQuotas(diffSyncs);

        // then
        // the gauge retrieved is the sum of the gauges from instance 1 and 2 and the diff
        UserQuotaAggregate result = quotaRepository.fetchDownloadQuotaSum(email);
        assertEquals(
            quotaOnInstance1 + quotaOnInstance2 + quotaDiffOnInstance1,
            result.getCounter().longValue()
        );
    }

    @Test
    public void flushSyncAndRefreshQuotas_should_update_instances_rates_gauges_and_return_sum_across_instances() {
        Random rand = new Random();

        // given
        // a user,
        String email = "foo@bar.com";

        // two instances,
        String instanceId1 = UUID.randomUUID().toString();
        String instanceId2 = UUID.randomUUID().toString();

        long rateOnInstance1 = rand.nextInt(10_000);
        long rateOnInstance2 = rand.nextInt(10_000);
        quotaRepository.upsertOrCombineDownloadRate(instanceId1, email, rateOnInstance1, LocalDateTime.now());
        quotaRepository.upsertOrCombineDownloadRate(instanceId2, email, rateOnInstance2, LocalDateTime.now());

        // and a diff on one of the two instances
        DownloadQuotaLimits downloadQuotaStub = new DownloadQuotaLimits(getDefaultTenant(), email, (long) rand.nextInt(10_000), (long) rand.nextInt(10_000));
        long rateDiffOnInstance1 = rand.nextInt(10_000);
        Map<String, QuotaManagerImpl.DiffSync> diffSyncs =
            new HashMap<String, QuotaManagerImpl.DiffSync>() {{
                put(email, new QuotaManagerImpl.DiffSync(rateDiffOnInstance1, 0L));
            }};

        // when
        // this instance flushes its diff and refreshes the global gauge value across instances
        quotaManager.flushSyncAndRefreshQuotas(diffSyncs);

        // then
        // the gauge retrieved is the sum of the gauges from instance 1 and 2 and the diff
        UserRateAggregate result = quotaRepository.fetchDownloadRatesSum(email);
        assertEquals(
            rateOnInstance1 + rateOnInstance2 + rateDiffOnInstance1,
            result.getGauge().longValue()
        );
    }
}

