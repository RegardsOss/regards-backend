package fr.cnes.regards.modules.storage.service.file.download;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.domain.database.*;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import org.assertj.core.api.ThrowableAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class QuotaManagerImplTest {

    private static final String TENANT = "default";

    private long gaugeExpirationTick = 600;

    private long gaugeSyncTick = 30;

    @Mock private ThreadPoolTaskScheduler gaugeExpirationTickingScheduler;

    @Mock private ThreadPoolTaskScheduler gaugeSyncTickingScheduler;

    @Mock private IDownloadQuotaRepository quotaRepository;

    @Mock private ITenantResolver tenantResolver;

    @Mock private IRuntimeTenantResolver runtimeTenantResolver;

    @Mock private ApplicationContext applicationContext;

    @Mock private Environment env;

    private QuotaManagerImpl quotaManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doNothing()
            .when(runtimeTenantResolver)
            .forceTenant(anyString());
        doReturn(TENANT)
            .when(runtimeTenantResolver)
            .getTenant();

        quotaManager = spy(
            new QuotaManagerImpl(
                gaugeExpirationTick,
                gaugeSyncTick,
                gaugeExpirationTickingScheduler,
                gaugeSyncTickingScheduler,
                quotaRepository,
                tenantResolver,
                runtimeTenantResolver,
                applicationContext,
                env
            )
        );

        quotaManager.setSelf(quotaManager);
        quotaManager.setUserDiffsByTenant(new java.util.HashMap<>());//HashMap.empty());
        quotaManager.setDiffsAccumulatorByTenant(new java.util.HashMap<>());//HashMap.empty());
//        quotaService.setCache(new AtomicReference<>(HashMap.empty()));
    }

    @After
    public void tearDown() {
        quotaRepository.deleteAll();
        Mockito.clearInvocations(quotaRepository);
    }

    @Test
    public void get_should_upsert_gauge_at_zero_and_update_cache() throws ExecutionException, InterruptedException {
        String instanceId = UUID.randomUUID().toString();
        String email = "foo@bar.com";
        Long quota = 5L;
        Long rate = 5L;
        DownloadQuotaLimits downloadQuota = new DownloadQuotaLimits(TENANT, email, quota, rate);
        quotaManager.setInstanceId(instanceId);
//        Map<String, AtomicReference<Map<DownloadQuota, QuotaManagerImpl.UserDiffs>>> cache =
//            HashMap.of(
//                TENANT,
//                new AtomicReference<>(HashMap.empty())
//            );
        Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        quotaManager.setUserDiffsByTenant(new java.util.HashMap<String, Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs>>() {{
            put(TENANT, cache);
        }});

        long stubGauge = 0L;
        long stubCounter = 0L;

        when(quotaRepository.upsertOrCombineDownloadRate(eq(instanceId), eq(email), eq(0L), any()))
            .thenReturn(new UserDownloadRate(instanceId, TENANT, email, stubGauge, LocalDateTime.now().plusSeconds(gaugeSyncTick)));
        when(quotaRepository.upsertOrCombineDownloadQuota(eq(instanceId), eq(email), eq(0L)))
            .thenReturn(new UserDownloadQuota(instanceId, TENANT, email, stubCounter));
        when(quotaRepository.fetchDownloadRatesSum(email))
            .thenReturn(new UserRateAggregate(stubGauge));
        when(quotaRepository.fetchDownloadQuotaSum(email))
            .thenReturn(new UserQuotaAggregate(stubCounter));

        Tuple2<UserQuotaAggregate, UserRateAggregate> quotaAndRate = quotaManager.get(downloadQuota).get();
//        QuotaManagerImpl.UserDiffs userDiffs = cache.get(TENANT).get().get().get(downloadQuota).get();
        QuotaManagerImpl.UserDiffs userDiffs = cache.getIfPresent(downloadQuota);

        assertEquals(stubCounter, quotaAndRate._1.getCounter().longValue());
        assertEquals(stubGauge, quotaAndRate._2.getGauge().longValue());
        assertEquals(stubCounter, userDiffs.getTotalQuota().longValue());
        assertEquals(stubGauge, userDiffs.getTotalRate().longValue());
    }

    @Test
    public void increment_should_update_cache() {
        String instanceId = UUID.randomUUID().toString();
        String email = "foo@bar.com";
        Long quota = 5L;
        Long rate = 5L;
        DownloadQuotaLimits downloadQuota = new DownloadQuotaLimits(TENANT, email, quota, rate);
        quotaManager.setInstanceId(instanceId);
//        Map<String, AtomicReference<Map<DownloadQuota, QuotaManagerImpl.UserDiffs>>> cache =
//            HashMap.of(
//                TENANT,
//                new AtomicReference<>(
//                    HashMap.of(
//                        downloadQuota,
//                        new QuotaManagerImpl.UserDiffs(
//                            new UserRate(email, 0L), 0L,
//                            new UserQuota(email, 0L), 0L)
//                    )
//                )
//            );
        Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        cache.put(downloadQuota, new QuotaManagerImpl.UserDiffs(
            new UserRateAggregate(0L), 0L,
            new UserQuotaAggregate( 0L), 0L));
        quotaManager.setUserDiffsByTenant(new java.util.HashMap<String, Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs>>() {{
            put(TENANT, cache);
        }});

        quotaManager.increment(downloadQuota);

//        QuotaManagerImpl.UserDiffs supposedlyCachedDiffs = cache.get(TENANT).get().get().get(downloadQuota).get();
        QuotaManagerImpl.UserDiffs supposedlyCachedDiffs = cache.getIfPresent(downloadQuota);
        assertEquals(0L, supposedlyCachedDiffs.getQuota().getCounter().longValue());
        assertEquals(1L, supposedlyCachedDiffs.getQuotaDiff().longValue());
        assertEquals(0L, supposedlyCachedDiffs.getRate().getGauge().longValue());
        assertEquals(1L, supposedlyCachedDiffs.getRateDiff().longValue());
    }

    @Test
    public void increment_should_fail_if_quota_not_cached() {
        String instanceId = UUID.randomUUID().toString();
        String email = "foo@bar.com";
        Long quota = 5L;
        Long rate = 5L;
        DownloadQuotaLimits downloadQuota = new DownloadQuotaLimits(TENANT, email, quota, rate);
        quotaManager.setInstanceId(instanceId);
//        Map<String, AtomicReference<Map<DownloadQuota, QuotaManagerImpl.UserDiffs>>> cache =
//            HashMap.empty();
        Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        quotaManager.setUserDiffsByTenant(new java.util.HashMap<String, Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs>>() {{
            put(TENANT, cache);
        }});

        ThrowableAssert.ThrowingCallable throwing = () -> quotaManager.increment(downloadQuota);

        assertThatThrownBy(throwing)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void decrement_should_update_cache() {
        String instanceId = UUID.randomUUID().toString();
        String email = "foo@bar.com";
        Long quota = 5L;
        Long rate = 5L;
        DownloadQuotaLimits downloadQuota = new DownloadQuotaLimits(TENANT, email, quota, rate);
        quotaManager.setInstanceId(instanceId);
//        Map<String, AtomicReference<Map<DownloadQuota, QuotaManagerImpl.UserDiffs>>> cache =
//            HashMap.of(
//                TENANT,
//                new AtomicReference<>(
//                    HashMap.of(
//                        downloadQuota,
//                        new QuotaManagerImpl.UserDiffs(
//                            new UserRate(email, 0L), 0L,
//                            new UserQuota(email, 0L), 0L)
//                    )
//                )
//            );
//        quotaManager.setUserDiffsByTenant(cache);
        Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        cache.put(downloadQuota,
            new QuotaManagerImpl.UserDiffs(
                new UserRateAggregate(0L), 0L,
                new UserQuotaAggregate(0L), 0L)
        );
        quotaManager.setUserDiffsByTenant(new java.util.HashMap<String, Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs>>() {{
            put(TENANT, cache);
        }});

        quotaManager.decrement(downloadQuota);

//        QuotaManagerImpl.UserDiffs supposedlyCachedDiffs = cache.get(TENANT).get().get().get(downloadQuota).get();
        QuotaManagerImpl.UserDiffs supposedlyCachedDiffs = cache.getIfPresent(downloadQuota);
        assertEquals(0L, supposedlyCachedDiffs.getQuota().getCounter().longValue());
        assertEquals(0L, supposedlyCachedDiffs.getQuotaDiff().longValue());
        assertEquals(0L, supposedlyCachedDiffs.getRate().getGauge().longValue());
        assertEquals(-1L, supposedlyCachedDiffs.getRateDiff().longValue());
    }

    @Test
    public void decrement_should_fail_if_quota_not_cached() {
        String instanceId = UUID.randomUUID().toString();
        String email = "foo@bar.com";
        Long quota = 5L;
        Long rate = 5L;
        DownloadQuotaLimits downloadQuota = new DownloadQuotaLimits(TENANT, email, quota, rate);
        quotaManager.setInstanceId(instanceId);
//        Map<String, AtomicReference<Map<DownloadQuota, QuotaManagerImpl.UserDiffs>>> cache =
//            HashMap.empty();
//        quotaManager.setUserDiffsByTenant(cache);
        Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        quotaManager.setUserDiffsByTenant(new java.util.HashMap<String, Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs>>() {{
            put(TENANT, cache);
        }});

        ThrowableAssert.ThrowingCallable throwing = () -> quotaManager.decrement(downloadQuota);

        assertThatThrownBy(throwing)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void syncGauges_should_update_quota_counters_and_rate_gauges() {
        Random rand = new Random();

        // given
        String email = "foo@bar.com";

        String instanceId = UUID.randomUUID().toString();
        quotaManager.setInstanceId(instanceId);

        // assuming there already is a quota counter and rate gauge in DB+cache (with some diffs to sync)
        long globalQuota = rand.nextInt(10_000);
        long globalRate = rand.nextInt(10_000);
        long quotaDiff = rand.nextInt(10_000);
        long rateDiff = rand.nextInt(10_000);
        DownloadQuotaLimits downloadQuotaStub = new DownloadQuotaLimits(TENANT, email, (long) rand.nextInt(10_000), (long) rand.nextInt(10_000));
//        AtomicReference<Map<DownloadQuota, QuotaManagerImpl.UserDiffs>> tenantDiffs =
//            new AtomicReference<>(
//                HashMap.of(
//                    downloadQuotaStub,
//                    new QuotaManagerImpl.UserDiffs(
//                        new UserRate(email, globalRate),
//                        rateDiff,
//                        new UserQuota(email, globalQuota),
//                        quotaDiff
//                    )
//                )
//            );
//        Map<String, AtomicReference<Map<DownloadQuota, QuotaManagerImpl.UserDiffs>>> cache =
//            HashMap.of(
//                TENANT,
//                tenantDiffs
//            );
//        quotaManager.setUserDiffsByTenant(cache);
        Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        cache.put(
            downloadQuotaStub,
            new QuotaManagerImpl.UserDiffs(
                new UserRateAggregate(globalRate),
                rateDiff,
                new UserQuotaAggregate(globalQuota),
                quotaDiff
            )
        );
        quotaManager.setUserDiffsByTenant(new java.util.HashMap<String, Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs>>() {{
            put(TENANT, cache);
        }});
        quotaManager.setDiffsAccumulatorByTenant(new java.util.HashMap<String, java.util.Map<DownloadQuotaLimits, QuotaManagerImpl.DiffSync>>() {{
            put(TENANT, new java.util.HashMap<>());
        }});

        // if an incr happens during the sync
        doAnswer(a -> {
            // cause side effect on the current diffs to simulate their incr during a sync
            cache.asMap().computeIfPresent(downloadQuotaStub, (q, d) ->
                QuotaManagerImpl.UserDiffs.incrementQuotaAndRateDiffs(d)
            );
//            tenantDiffs.updateAndGet(m ->
//                m.computeIfPresent(downloadQuotaStub, (q, d) ->
//                        QuotaManagerImpl.UserDiffs.incrementQuotaAndRateDiffs(d)
//                )._2);

            // return as if a real db call has been issued
//            return HashMap.of(
//                downloadQuotaStub,
//                new QuotaManagerImpl.UserDiffs(
//                    new UserRate(email, globalRate + rateDiff),
//                    0L,
//                    new UserQuota(email, globalQuota + quotaDiff),
//                    0L
//                )
//            );
            return new java.util.HashMap<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs>() {{
                put(downloadQuotaStub,
                    new QuotaManagerImpl.UserDiffs(
                        new UserRateAggregate(globalRate + rateDiff),
                        0L,
                        new UserQuotaAggregate(globalQuota + quotaDiff),
                        0L
                    ));
            }};
        }).when(quotaManager)
            .flushSyncAndRefreshQuotas(any());

        // when
        // the sync runs
        quotaManager.syncGauges(TENANT);

        // then
//        QuotaManagerImpl.UserDiffs userDiffs = tenantDiffs.get().get(downloadQuotaStub).get();
        QuotaManagerImpl.UserDiffs userDiffs = cache.getIfPresent(downloadQuotaStub);
        // the current cached global has changed
        assertEquals(globalRate + rateDiff, userDiffs.getRate().getGauge().longValue());
        assertEquals(globalQuota + quotaDiff, userDiffs.getQuota().getCounter().longValue());
        // the cached diffs have increased
        assertEquals(1L, userDiffs.getRateDiff().longValue());
        assertEquals(1L, userDiffs.getQuotaDiff().longValue());
        // the diffs accumulator hasn't changed
//        assertTrue(quotaManager.getDiffsAccumulatorByTenant().get(TENANT).get().get(downloadQuotaStub).isEmpty());
        assertTrue(Option.of(quotaManager.getDiffsAccumulatorByTenant().get(TENANT).get(downloadQuotaStub)).isEmpty());

        // and flushSyncAndRefreshQuotas has been called with the correct quotaDiff and rateDiff to sync
//        ArgumentCaptor<Map<DownloadQuota, QuotaManagerImpl.DiffSync>> flushSyncAndRefreshArgumentCaptor =
//            ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<java.util.Map<DownloadQuotaLimits, QuotaManagerImpl.DiffSync>> flushSyncAndRefreshArgumentCaptor =
            ArgumentCaptor.forClass(java.util.Map.class);
        verify(quotaManager).flushSyncAndRefreshQuotas(flushSyncAndRefreshArgumentCaptor.capture());
        QuotaManagerImpl.DiffSync argument = flushSyncAndRefreshArgumentCaptor.getValue().get(downloadQuotaStub);
        assertEquals(rateDiff, argument.getRateDiff().longValue());
        assertEquals(quotaDiff, argument.getQuotaDiff().longValue());
    }

    @Test
    public void syncGauges_should_accumulate_diffs_when_flushSync_fails() {
        Random rand = new Random();

        // given
        String email = "foo@bar.com";

        String instanceId = UUID.randomUUID().toString();
        quotaManager.setInstanceId(instanceId);

        // assuming there already is a quota counter and rate gauge in DB+cache (with some diffs to sync)
        long globalQuota = rand.nextInt(10_000);
        long globalRate = rand.nextInt(10_000);
        long quotaDiff = rand.nextInt(10_000);
        long rateDiff = rand.nextInt(10_000);
        DownloadQuotaLimits downloadQuotaStub = new DownloadQuotaLimits(TENANT, email, (long) rand.nextInt(10_000), (long) rand.nextInt(10_000));
//        AtomicReference<Map<DownloadQuota, QuotaManagerImpl.UserDiffs>> tenantDiffs =
//            new AtomicReference<>(
//                HashMap.of(
//                    downloadQuotaStub,
//                    new QuotaManagerImpl.UserDiffs(
//                        new UserRate(email, globalRate),
//                        rateDiff,
//                        new UserQuota(email, globalQuota),
//                        quotaDiff
//                    )
//                )
//            );
//        Map<String, AtomicReference<Map<DownloadQuota, QuotaManagerImpl.UserDiffs>>> cache =
//            HashMap.of(
//                TENANT,
//                tenantDiffs
//            );
        Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        cache.put(
            downloadQuotaStub,
            new QuotaManagerImpl.UserDiffs(
                new UserRateAggregate(globalRate),
                rateDiff,
                new UserQuotaAggregate(globalQuota),
                quotaDiff
            )
        );
        quotaManager.setUserDiffsByTenant(new java.util.HashMap<String, Cache<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs>>() {{
            put(TENANT, cache);
        }});
        quotaManager.setDiffsAccumulatorByTenant(new java.util.HashMap<String, java.util.Map<DownloadQuotaLimits, QuotaManagerImpl.DiffSync>>() {{
            put(TENANT, new java.util.HashMap<>());
        }});

        // when a first sync fails
        Throwable expected = new RuntimeException("expected");
        doAnswer(a -> {
            // cause side effect on the current diffs to simulate their incr during a sync
            cache.asMap().computeIfPresent(downloadQuotaStub, (q, d) ->
                QuotaManagerImpl.UserDiffs.incrementQuotaAndRateDiffs(d)
            );
//            tenantDiffs.updateAndGet(m ->
//                m.computeIfPresent(downloadQuotaStub, (q, d) ->
//                    QuotaManagerImpl.UserDiffs.incrementQuotaAndRateDiffs(d)
//                )._2);

            throw expected;
        }).when(quotaManager)
            .flushSyncAndRefreshQuotas(any());
        assertThatThrownBy(() -> quotaManager.syncGauges(TENANT))
            .isEqualTo(expected);

        // assert that
//        QuotaManagerImpl.UserDiffs userDiffs = tenantDiffs.get().get(downloadQuotaStub).get();
        QuotaManagerImpl.UserDiffs userDiffs = cache.getIfPresent(downloadQuotaStub);
        // the current cached global hasn't changed
        assertEquals(globalRate, userDiffs.getRate().getGauge().longValue());
        assertEquals(globalQuota, userDiffs.getQuota().getCounter().longValue());
        // the cached diffs have increased
        assertEquals(1L, userDiffs.getRateDiff().longValue());
        assertEquals(1L, userDiffs.getQuotaDiff().longValue());
        // the diffs accumulator has accumulated the diffs that wasn't synced
        QuotaManagerImpl.DiffSync diffsAcc = quotaManager.getDiffsAccumulatorByTenant()
            .get(TENANT)
            .get(downloadQuotaStub);
//        QuotaManagerImpl.DiffSync diffsAcc = quotaManager.getDiffsAccumulatorByTenant()
//            .get(TENANT).get()
//            .get(downloadQuotaStub).get();
        assertEquals(rateDiff, diffsAcc.getRateDiff().longValue());
        assertEquals(quotaDiff, diffsAcc.getQuotaDiff().longValue());

        // when a second sync succeeds
        doReturn(new java.util.HashMap<DownloadQuotaLimits, QuotaManagerImpl.UserDiffs>() {{
            put(downloadQuotaStub,
                new QuotaManagerImpl.UserDiffs(
                    new UserRateAggregate(globalRate + rateDiff + 1L),
                    0L,
                    new UserQuotaAggregate(globalQuota + quotaDiff + 1L),
                    0L
                )
            );
        }}).when(quotaManager)
            .flushSyncAndRefreshQuotas(any());
        quotaManager.syncGauges(TENANT);

        // then
//        userDiffs = tenantDiffs.get().get(downloadQuotaStub).get();
        userDiffs = cache.getIfPresent(downloadQuotaStub);
        // the current cached globals have changed
        assertEquals(1L + globalRate + rateDiff, userDiffs.getRate().getGauge().longValue());
        assertEquals(1L + globalQuota + quotaDiff, userDiffs.getQuota().getCounter().longValue());
        // the cached diffs are back to zero
        assertEquals(0L, userDiffs.getRateDiff().longValue());
        assertEquals(0L, userDiffs.getQuotaDiff().longValue());
        // the diffs accumulator has been reset
//        assertTrue(quotaManager.getDiffsAccumulatorByTenant().get(TENANT).get().get(downloadQuotaStub).isEmpty());
        assertTrue(Option.of(quotaManager.getDiffsAccumulatorByTenant().get(TENANT).get(downloadQuotaStub)).isEmpty());

        // and flushSyncAndRefreshQuotas has been called with the correct quotaDiff and rateDiff to sync
        ArgumentCaptor<java.util.Map<DownloadQuotaLimits, QuotaManagerImpl.DiffSync>> flushSyncAndRefreshArgumentCaptor =
            ArgumentCaptor.forClass(java.util.Map.class);
//        ArgumentCaptor<Map<DownloadQuota, QuotaManagerImpl.DiffSync>> flushSyncAndRefreshArgumentCaptor =
//            ArgumentCaptor.forClass(Map.class);
        verify(quotaManager, times(2)).flushSyncAndRefreshQuotas(flushSyncAndRefreshArgumentCaptor.capture());
        QuotaManagerImpl.DiffSync argument = flushSyncAndRefreshArgumentCaptor.getAllValues().get(1).get(downloadQuotaStub);
        assertEquals(1L + rateDiff, argument.getRateDiff().longValue());
        assertEquals(1L + quotaDiff, argument.getQuotaDiff().longValue());
    }
}
