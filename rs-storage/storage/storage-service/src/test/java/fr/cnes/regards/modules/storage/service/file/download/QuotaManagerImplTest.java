package fr.cnes.regards.modules.storage.service.file.download;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.framework.amqp.IPublisher;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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

    @Mock
    private ThreadPoolTaskScheduler gaugeExpirationTickingScheduler;

    @Mock
    private ThreadPoolTaskScheduler gaugeSyncTickingScheduler;

    @Mock
    private IDownloadQuotaRepository quotaRepository;

    @Mock
    private ITenantResolver tenantResolver;

    @Mock
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private IPublisher publisher;

    @Mock
    private Environment env;

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

        quotaManager =
                spy(new QuotaManagerImpl(gaugeExpirationTickingScheduler, gaugeSyncTickingScheduler, quotaRepository, tenantResolver, runtimeTenantResolver, publisher, env, quotaManager));

        ReflectionTestUtils.setField(quotaManager, "self", quotaManager);
        quotaManager.setUserDiffsByTenant(new HashMap<>());//HashMap.empty());
        quotaManager.setDiffsAccumulatorByTenant(new HashMap<>());//HashMap.empty());
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
        Cache<String, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        quotaManager.setUserDiffsByTenant(new HashMap<String, Cache<String, QuotaManagerImpl.UserDiffs>>() {{
            put(TENANT, cache);
        }});

        long stubGauge = 0L;
        long stubCounter = 0L;

        when(quotaRepository.upsertOrCombineDownloadRate(eq(instanceId), eq(email), eq(0L), any()))
                .thenReturn(new UserDownloadRate(instanceId, TENANT, email, stubGauge, LocalDateTime.now().plusSeconds(30)));
        when(quotaRepository.upsertOrCombineDownloadQuota(eq(instanceId), eq(email), eq(0L)))
                .thenReturn(new UserDownloadQuota(instanceId, TENANT, email, stubCounter));
        when(quotaRepository.fetchDownloadRatesSum(email))
                .thenReturn(new UserRateAggregate(stubGauge));
        when(quotaRepository.fetchDownloadQuotaSum(email))
                .thenReturn(new UserQuotaAggregate(stubCounter));

        Tuple2<UserQuotaAggregate, UserRateAggregate> quotaAndRate = quotaManager.get(downloadQuota);
        QuotaManagerImpl.UserDiffs userDiffs = cache.getIfPresent(email);

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
        Cache<String, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        cache.put(email, new QuotaManagerImpl.UserDiffs(
            new UserRateAggregate(0L), 0L,
            new UserQuotaAggregate( 0L), 0L));
        quotaManager.setUserDiffsByTenant(new HashMap<String, Cache<String, QuotaManagerImpl.UserDiffs>>() {{
            put(TENANT, cache);
        }});

        quotaManager.increment(downloadQuota);

        QuotaManagerImpl.UserDiffs supposedlyCachedDiffs = cache.getIfPresent(email);
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
        Cache<String, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        quotaManager.setUserDiffsByTenant(new HashMap<String, Cache<String, QuotaManagerImpl.UserDiffs>>() {{
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
        Cache<String, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        cache.put(email,
            new QuotaManagerImpl.UserDiffs(
                new UserRateAggregate(0L), 0L,
                new UserQuotaAggregate(0L), 0L)
        );
        quotaManager.setUserDiffsByTenant(new HashMap<String, Cache<String, QuotaManagerImpl.UserDiffs>>() {{
            put(TENANT, cache);
        }});

        quotaManager.decrement(downloadQuota);

        QuotaManagerImpl.UserDiffs supposedlyCachedDiffs = cache.getIfPresent(email);
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
        Cache<String, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        quotaManager.setUserDiffsByTenant(new HashMap<String, Cache<String, QuotaManagerImpl.UserDiffs>>() {{
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
        Cache<String, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        cache.put(
            email,
            new QuotaManagerImpl.UserDiffs(
                new UserRateAggregate(globalRate),
                rateDiff,
                new UserQuotaAggregate(globalQuota),
                quotaDiff
            )
        );
        quotaManager.setUserDiffsByTenant(new HashMap<String, Cache<String, QuotaManagerImpl.UserDiffs>>() {{
            put(TENANT, cache);
        }});
        quotaManager.setDiffsAccumulatorByTenant(new HashMap<String, Map<String, QuotaManagerImpl.DiffSync>>() {{
            put(TENANT, new HashMap<>());
        }});

        // if an incr happens during the sync
        doAnswer(a -> {
            // cause side effect on the current diffs to simulate their incr during a sync
            cache.asMap().computeIfPresent(email, (q, d) ->
                QuotaManagerImpl.UserDiffs.incrementQuotaAndRateDiffs(d)
            );
            return new HashMap<String, QuotaManagerImpl.UserDiffs>() {{
                put(email,
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
        quotaManager.syncDiffs(TENANT);

        // then
        QuotaManagerImpl.UserDiffs userDiffs = cache.getIfPresent(email);
        // the current cached global has changed
        assertEquals(globalRate + rateDiff, userDiffs.getRate().getGauge().longValue());
        assertEquals(globalQuota + quotaDiff, userDiffs.getQuota().getCounter().longValue());
        // the cached diffs have increased
        assertEquals(1L, userDiffs.getRateDiff().longValue());
        assertEquals(1L, userDiffs.getQuotaDiff().longValue());
        // the diffs accumulator hasn't changed
        assertTrue(Option.of(quotaManager.getDiffsAccumulatorByTenant().get(TENANT).get(email)).isEmpty());

        // and flushSyncAndRefreshQuotas has been called with the correct quotaDiff and rateDiff to sync
        ArgumentCaptor<Map<String, QuotaManagerImpl.DiffSync>> flushSyncAndRefreshArgumentCaptor =
            ArgumentCaptor.forClass(Map.class);
        verify(quotaManager).flushSyncAndRefreshQuotas(flushSyncAndRefreshArgumentCaptor.capture());
        QuotaManagerImpl.DiffSync argument = flushSyncAndRefreshArgumentCaptor.getValue().get(email);
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
        Cache<String, QuotaManagerImpl.UserDiffs> cache = Caffeine.newBuilder().build();
        cache.put(
            email,
            new QuotaManagerImpl.UserDiffs(
                new UserRateAggregate(globalRate),
                rateDiff,
                new UserQuotaAggregate(globalQuota),
                quotaDiff
            )
        );
        quotaManager.setUserDiffsByTenant(new HashMap<String, Cache<String, QuotaManagerImpl.UserDiffs>>() {{
            put(TENANT, cache);
        }});
        quotaManager.setDiffsAccumulatorByTenant(new HashMap<String, Map<String, QuotaManagerImpl.DiffSync>>() {{
            put(TENANT, new HashMap<>());
        }});

        // when a first sync fails
        Throwable expected = new RuntimeException("expected");
        doAnswer(a -> {
            // cause side effect on the current diffs to simulate their incr during a sync
            cache.asMap().computeIfPresent(email, (q, d) ->
                QuotaManagerImpl.UserDiffs.incrementQuotaAndRateDiffs(d)
            );

            throw expected;
        }).when(quotaManager)
            .flushSyncAndRefreshQuotas(any());
        assertThatThrownBy(() -> quotaManager.syncDiffs(TENANT))
            .isEqualTo(expected);

        // assert that
        QuotaManagerImpl.UserDiffs userDiffs = cache.getIfPresent(email);
        // the current diffs have been added to the cached globals during the sync
        assertEquals(globalRate + rateDiff, userDiffs.getRate().getGauge().longValue());
        assertEquals(globalQuota + quotaDiff, userDiffs.getQuota().getCounter().longValue());
        // the cached diffs have increased
        assertEquals(1L, userDiffs.getRateDiff().longValue());
        assertEquals(1L, userDiffs.getQuotaDiff().longValue());
        // the diffs accumulator has accumulated the diffs that wasn't synced
        QuotaManagerImpl.DiffSync diffsAcc = quotaManager.getDiffsAccumulatorByTenant()
            .get(TENANT)
            .get(email);
        assertEquals(rateDiff, diffsAcc.getRateDiff().longValue());
        assertEquals(quotaDiff, diffsAcc.getQuotaDiff().longValue());

        // when a second sync succeeds
        doReturn(new HashMap<String, QuotaManagerImpl.UserDiffs>() {{
            put(email,
                new QuotaManagerImpl.UserDiffs(
                    new UserRateAggregate(globalRate + rateDiff + 1L),
                    0L,
                    new UserQuotaAggregate(globalQuota + quotaDiff + 1L),
                    0L
                )
            );
        }}).when(quotaManager)
            .flushSyncAndRefreshQuotas(any());
        quotaManager.syncDiffs(TENANT);

        // then
        userDiffs = cache.getIfPresent(email);
        // the current cached globals have changed
        assertEquals(1L + globalRate + rateDiff, userDiffs.getRate().getGauge().longValue());
        assertEquals(1L + globalQuota + quotaDiff, userDiffs.getQuota().getCounter().longValue());
        // the cached diffs are back to zero
        assertEquals(0L, userDiffs.getRateDiff().longValue());
        assertEquals(0L, userDiffs.getQuotaDiff().longValue());
        // the diffs accumulator has been reset
        assertTrue(Option.of(quotaManager.getDiffsAccumulatorByTenant().get(TENANT).get(email)).isEmpty());

        // and flushSyncAndRefreshQuotas has been called with the correct quotaDiff and rateDiff to sync
        ArgumentCaptor<Map<String, QuotaManagerImpl.DiffSync>> flushSyncAndRefreshArgumentCaptor =
            ArgumentCaptor.forClass(Map.class);
        verify(quotaManager, times(2)).flushSyncAndRefreshQuotas(flushSyncAndRefreshArgumentCaptor.capture());
        QuotaManagerImpl.DiffSync argument = flushSyncAndRefreshArgumentCaptor.getAllValues().get(1).get(email);
        assertEquals(1L + rateDiff, argument.getRateDiff().longValue());
        assertEquals(1L + quotaDiff, argument.getQuotaDiff().longValue());
    }
}
