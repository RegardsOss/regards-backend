package fr.cnes.regards.modules.storage.service.file.download;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserQuotaAggregate;
import fr.cnes.regards.modules.storage.domain.database.UserRateAggregate;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static fr.cnes.regards.modules.storage.service.file.download.QuotaManagerConfiguration.RATE_EXPIRATION_TICKING_SCHEDULER;
import static fr.cnes.regards.modules.storage.service.file.download.QuotaManagerConfiguration.SYNC_TICKING_SCHEDULER;

@Component
@MultitenantTransactional
public class QuotaManagerImpl implements IQuotaManager {

    private static Logger LOGGER = LoggerFactory.getLogger(QuotaManagerImpl.class);

    private long rateExpirationTick;

    private long syncTick;

    private ThreadPoolTaskScheduler rateExpirationTickingScheduler;

    private ThreadPoolTaskScheduler syncTickingScheduler;

    private IDownloadQuotaRepository quotaRepository;

    private ITenantResolver tenantResolver;

    private IRuntimeTenantResolver runtimeTenantResolver;

    private ApplicationContext applicationContext;

    private Environment env;

    private QuotaManagerImpl self;

    private final AtomicBoolean onRateExpirationScheduler = new AtomicBoolean(false);

    private final AtomicBoolean onSyncScheduler = new AtomicBoolean(false);

//    private Map<String, AtomicReference<Map<DownloadQuota, UserDiffs>>> userDiffsByTenant =
//        HashMap.empty();

    private java.util.Map<String, Cache<DownloadQuotaLimits, UserDiffs>> userDiffsByTenant=
        new java.util.HashMap<>();

    private java.util.Map<String, java.util.Map<DownloadQuotaLimits, DiffSync>> diffsAccumulatorByTenant =
        new java.util.HashMap<>();

    private final ScheduledThreadPoolExecutor executor =
        new ScheduledThreadPoolExecutor(1, new QuotaThreadFactory(), (r, e) -> LOGGER.error("{} rejected {}", e, r));

    private String instanceId = UUID.randomUUID().toString();

    public QuotaManagerImpl() {}

    @Autowired
    public QuotaManagerImpl(
        @Value("${regards.admin.rate.expiration.tick:120}") long rateExpirationTick,
        @Value("${regards.admin.quota.sync.tick:30}") long syncTick,
        @Qualifier(RATE_EXPIRATION_TICKING_SCHEDULER) ThreadPoolTaskScheduler rateExpirationTickingScheduler,
        @Qualifier(SYNC_TICKING_SCHEDULER) ThreadPoolTaskScheduler syncTickingScheduler,
        /*@Qualifier("plop") */IDownloadQuotaRepository quotaRepository,
        ITenantResolver tenantResolver,
        IRuntimeTenantResolver runtimeTenantResolver,
        ApplicationContext applicationContext,
        Environment env
    ) {
        this.rateExpirationTick = rateExpirationTick;
        this.syncTick = syncTick;
        this.rateExpirationTickingScheduler = rateExpirationTickingScheduler;
        this.syncTickingScheduler = syncTickingScheduler;
        this.quotaRepository = quotaRepository;
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.applicationContext = applicationContext;
        this.env = env;
    }

    @VisibleForTesting
    protected void setSelf(QuotaManagerImpl self) {
        this.self = self;
    }

    @VisibleForTesting
//    public void setUserDiffsByTenant(Map<String, AtomicReference<Map<DownloadQuota, UserDiffs>>> userDiffsByTenant) {
    public void setUserDiffsByTenant(java.util.Map<String, Cache<DownloadQuotaLimits, UserDiffs>> userDiffsByTenant) {
        this.userDiffsByTenant = userDiffsByTenant;
    }

    @VisibleForTesting
    public void setDiffsAccumulatorByTenant(java.util.Map<String, java.util.Map<DownloadQuotaLimits, DiffSync>> diffsAccumulator) {
        this.diffsAccumulatorByTenant = diffsAccumulator;
    }

    @VisibleForTesting
    public java.util.Map<String, java.util.Map<DownloadQuotaLimits, DiffSync>> getDiffsAccumulatorByTenant() {
        return diffsAccumulatorByTenant;
    }

    @VisibleForTesting
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @PostConstruct
    public void init() {
        self = applicationContext.getBean(QuotaManagerImpl.class);

        // init diffs and diffsAcc by tenant
        tenantResolver.getAllActiveTenants()
            .forEach(tenant -> {
                userDiffsByTenant.put(tenant, Caffeine.newBuilder()
                    .expireAfterAccess(30, TimeUnit.MINUTES)
                    .maximumSize(10_000)
                    .build()
                );
                diffsAccumulatorByTenant.put(tenant, new java.util.HashMap<>());
            })
//            .reduce(
//                Tuple.of(
//                    HashMap.<String, Cache<DownloadQuota, UserDiffs>>empty(),
//                    HashMap.<String, Map<DownloadQuota, DiffSync>>empty()
//                ),
//                (acc, tenant) -> acc
//                    .map1(diffs -> diffs.put(tenant, Caffeine.newBuilder()
//                        .expireAfterAccess(30, TimeUnit.MINUTES)
//                        .maximumSize(10_000)
//                        .build())
//                    )
//                    .map2(diffsAcc -> diffsAcc.put(tenant, HashMap.empty())),
//                (left, right) -> left
//            )
//            .map((diffs, diffsAcc) -> {
//                userDiffsByTenant = diffs;
//                diffsAccumulatorByTenant = diffsAcc;
//                return null;
//            })
        ;

        // start schedulers only if not in "noschedule" profile (i.e. disable schedulers for tests)
        boolean shouldStartSchedulers =
            Arrays.stream(env.getActiveProfiles())
                .distinct() // maybe useless but I don't trust
                .noneMatch(profile -> profile.equals("noschedule"));
        if (shouldStartSchedulers) {
            startGaugeExpirationScheduler();
            startGaugeSyncScheduler();
        }
    }

    private void startGaugeExpirationScheduler() {
        rateExpirationTickingScheduler.setThreadNamePrefix("PLOP");//FIXME
        rateExpirationTickingScheduler.scheduleWithFixedDelay(() -> {

            onRateExpirationScheduler.set(true);

            tenantResolver.getAllActiveTenants()
                .forEach(tenant -> {
                    runtimeTenantResolver.forceTenant(tenant);
                    try {
                        self.purgeExpiredGauges();
                    } finally {
                        runtimeTenantResolver.clearTenant();
                    }
                });

            onRateExpirationScheduler.set(false);
        }, Duration.ofSeconds(rateExpirationTick));
    }

    private void startGaugeSyncScheduler() {
        syncTickingScheduler.scheduleWithFixedDelay(() -> {

            onSyncScheduler.set(true);

            tenantResolver.getAllActiveTenants()
                .forEach(tenant -> {
                    runtimeTenantResolver.forceTenant(tenant);
                    try {
                        self.syncGauges(tenant);
                    } finally {
                        runtimeTenantResolver.clearTenant();
                    }
                });

            onSyncScheduler.set(false);
        }, Duration.ofSeconds(syncTick));
    }

    public void purgeExpiredGauges() {
        checkOnGaugeExpirationThread();

        quotaRepository.deleteExpiredRates();
    }

    public void syncGauges(String tenant) {
        // checkOnGaugeSyncThread(); FIXME can't test this!

        // sync will start, diffsAcc should be initialized if not found
        diffsAccumulatorByTenant.computeIfAbsent(tenant, t ->
            new java.util.HashMap<>());
//        diffsAccumulatorByTenant =
//            diffsAccumulatorByTenant.computeIfAbsent(tenant, t ->
//                HashMap.empty()
//            )._2;

        // swap current instance diff counters for tenant users to 0 and get previous value
        ConcurrentMap<DownloadQuotaLimits, UserDiffs> userDiffs = userDiffsByTenant.get(tenant).asMap();
        ImmutableMap<DownloadQuotaLimits, UserDiffs> currentSync = ImmutableMap.copyOf(userDiffs);
        userDiffs.replaceAll((q, ud) -> UserDiffs.renew(ud));
//        Map<DownloadQuota, UserDiffs> currentSync =
//            userDiffsByTenant.get(tenant).get()
//                .getAndUpdate(m ->
//                    m.mapValues(UserDiffs::renew)
//                );
        // now get/incr/decr can continue working on a new userDiff => no contention with this method

        // accumulate quota/rate diffs in case sync fails
        java.util.Map<DownloadQuotaLimits, DiffSync> diffsAcc = diffsAccumulatorByTenant.get(tenant);
        currentSync.forEach((q, u) -> {
            DiffSync r = new DiffSync(u.rateDiff, u.quotaDiff);
            diffsAcc.compute(q, (ignored, l) ->
                    (l == null) ? r : DiffSync.combine(l, r)
            );
        });
//        diffsAccumulatorByTenant =
//            diffsAccumulatorByTenant.computeIfPresent(tenant, (t, m) ->
//                m.merge(
//                    currentSync.mapValues(d -> new DiffSync(d.rateDiff, d.quotaDiff)),
//                    DiffSync::combine
//                )
//            )._2;

        // do sync quota/rate with db
        java.util.Map<DownloadQuotaLimits, UserDiffs> newUserDiffs =
            self.flushSyncAndRefreshQuotas(
                diffsAccumulatorByTenant.get(tenant)//.get()
            );

        // swap user gauges for fresh global gauge but keep working instance counter diff
        newUserDiffs.forEach((q, r) ->
            userDiffs.compute(q, (ignored, l) ->
                (r == null) ? l : UserDiffs.refresh(l, r)
            ));
//        userDiffsByTenant.get(tenant).get()
//            .updateAndGet(m ->
//                m.merge(
//                    newUserDiffs,
//                    UserDiffs::refresh
//                )
//            );

        // sync is finished, diffsAcc for current tenant can be cleared for next sync
        diffsAccumulatorByTenant.put(tenant, new HashMap<>());
//        diffsAccumulatorByTenant =
//            diffsAccumulatorByTenant.put(tenant, new HashMap<>());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public java.util.Map<DownloadQuotaLimits, UserDiffs> flushSyncAndRefreshQuotas(java.util.Map<DownloadQuotaLimits, DiffSync> diffSyncs) {
        // checkOnGaugeSyncThread(); FIXME can't test this!

        java.util.Map<DownloadQuotaLimits, UserDiffs> result = new HashMap<>();

        diffSyncs.forEach((quota, sync) -> {
            String email = quota.getEmail();
            // sync current instance quota/rate for user
            quotaRepository.upsertOrCombineDownloadQuota(instanceId, email, sync.quotaDiff);
            quotaRepository.upsertOrCombineDownloadRate(instanceId, email, sync.rateDiff, LocalDateTime.now().plusSeconds(syncTick));
            // and refresh its global quota/rate (select across all instances gauges)
            UserQuotaAggregate globalQuota = quotaRepository.fetchDownloadQuotaSum(email);
            UserRateAggregate globalRate = quotaRepository.fetchDownloadRatesSum(email);

            result.put(quota, new UserDiffs(globalRate, 0L, globalQuota, 0L));
        });

        return result;
    }

    @Override
    public Future<Tuple2<UserQuotaAggregate, UserRateAggregate>> get(DownloadQuotaLimits quota) {

        String tenant = quota.getTenant();
        String email = quota.getEmail();

        // TODO think about contention, multiple threads may be
        // TODO trying to increment/decrement some counters while this thread
        // TODO tries to upsert a gauge not in cache
        // TODO in this case the upsert may take a long time and this operation
        // TODO may be retried a lot before succeeding because of contantion
        // TODO on diffs which is changed much faster during incrs/decrs
        // TODO a solution could to partition the diffs: have an atomicref map
        // TODO of quotaKey to atomicref of diff
        // TODO this would create an indirection where get operations manage
        // TODO the atomicrefs map and contention is more fair between threads
        // TODO trying to upsert the "new" gauge
        // TODO and incrs/decrs operations would contend to update the individual
        // TODO diffs atomics only after they have been created since incr/decr
        // TODO before a get is a protocol violation

        // TODO maybe also use a lock per userPeriod to make sure an instance thread A doesn't try to
        // TODO upsert concurrently with thread B on same instance, it's a conflict that could be avoided
        // TODO and would prevent multiple DB transactions.

        // TODO this scheduler could be more than 1 thread. could even batch if atomics are "partitioned" to avoid contention

        return executor.submit(() -> {
            runtimeTenantResolver.forceTenant(tenant);
//            AtomicReference<Map<DownloadQuota, UserDiffs>> gaugeSyncs =
//                userDiffsByTenant.get(tenant).get();
            Cache<DownloadQuotaLimits, UserDiffs> gaugeSyncs = userDiffsByTenant.get(tenant);
            UserDiffs diffs = gaugeSyncs.get(quota, ignored -> {
                // create its current instance quota/rate if not exist (diff = 0L so operation is idempotent and thus CAS-loop safe)
                quotaRepository.upsertOrCombineDownloadQuota(instanceId, email, 0L);
                quotaRepository.upsertOrCombineDownloadRate(instanceId, email, 0L, LocalDateTime.now().plusSeconds(syncTick));
                // and get its global quota/rate (select across all instances gauges)
                UserQuotaAggregate globalQuota = quotaRepository.fetchDownloadQuotaSum(email);
                UserRateAggregate globalRate = quotaRepository.fetchDownloadRatesSum(email);
                return new UserDiffs(
                    globalRate,
                    0L,
                    globalQuota,
                    0L);
            });
            return Tuple.of(
                new UserQuotaAggregate(diffs.quota.getCounter() + diffs.quotaDiff),
                new UserRateAggregate(diffs.rate.getGauge() + diffs.rateDiff)
            );
        });
    }

    @Override
    public void increment(DownloadQuotaLimits quota) {
//        String tenant = quota.getTenant();

        Cache<DownloadQuotaLimits, UserDiffs> userDiffs = userDiffsByTenant.get(quota.getTenant());
        if (userDiffs.getIfPresent(quota) == null) {
            throw new IllegalStateException("Cannot incr before get");
        }
        userDiffs.asMap()
            .computeIfPresent(quota, (q, u) ->
                UserDiffs.incrementQuotaAndRateDiffs(u)
            );
//        userDiffsByTenant.get(tenant)
//            .getOrElseThrow(() -> new IllegalStateException("Cannot incr before get"))
//            .updateAndGet(m ->
//                m.computeIfPresent(quota, (q, u) ->
//                    UserDiffs.incrementQuotaAndRateDiffs(u)
//                )._2
//            );
    }

    @Override
    public void decrement(DownloadQuotaLimits quota) {
//        String tenant = quota.getTenant();

        Cache<DownloadQuotaLimits, UserDiffs> userDiffs = userDiffsByTenant.get(quota.getTenant());
        if (userDiffs.getIfPresent(quota) == null) {
            throw new IllegalStateException("Cannot decr before get");
        }
        userDiffs.asMap()
            .computeIfPresent(quota, (q, u) ->
                UserDiffs.decrementRateDiff(u)
            );
//        userDiffsByTenant.get(tenant)
//            .getOrElseThrow(() -> new IllegalStateException("Cannot decr before get"))
//            .updateAndGet(m ->
//                m.computeIfPresent(quota, (q, u) ->
//                    UserDiffs.decrementRateDiff(u)
//                )._2
//            );
    }

    private void checkThread() {
        if (Thread.currentThread().getThreadGroup() != QuotaThreadFactory.GROUP) {
            RuntimeException e =
                new RuntimeException("Should run inside the dedicated QuotaThread executor");
            LOGGER.error("Using QuotaManagerImpl internals in illegal thread", e);
            throw e;
        }
    }

    private void checkOnGaugeSyncThread() {
        if (!onSyncScheduler.get()) {
            RuntimeException e =
                new RuntimeException("Should run inside the dedicated gauge sync executor thread");
            LOGGER.error("Using QuotaManagerImpl internals in illegal thread", e);
            throw e;
        }
    }

    private void checkOnGaugeExpirationThread() {
        if (!onRateExpirationScheduler.get()) {
            RuntimeException e =
                new RuntimeException("Should run inside the dedicated gauge expiration executor thread");
            LOGGER.error("Using QuotaManagerImpl internals in illegal thread", e);
            throw e;
        }
    }

    private Runnable wrap(final Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (final Throwable t) {
                QuotaThreadFactory.UNCAUGHT_EXCEPTION_HANDLER.uncaughtException(Thread.currentThread(), t);
                throw t;
            }
        };
    }

    private static class QuotaThreadFactory implements ThreadFactory {
        private static final ThreadGroup GROUP = new ThreadGroup("quota-manager");
        private static final AtomicLong COUNT = new AtomicLong();
        public static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER = (t, e) ->
            LOGGER.error("Error in thread {}: {}", t.getName(), e.getMessage(), e);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(
                GROUP,
                r,
                String.format(
                    "%s-%d",
                    GROUP.getName(),
                    COUNT.incrementAndGet()
                )
            );
            thread.setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
            return thread;
        }
    }

    public static class UserDiffs {
        private final UserRateAggregate rate;

        private final Long rateDiff;

        private final UserQuotaAggregate quota;

        private final Long quotaDiff;

        public UserDiffs(UserRateAggregate rate, Long rateDiff, UserQuotaAggregate quota, Long quotaDiff) {
            this.rate = rate;
            this.rateDiff = rateDiff;
            this.quota = quota;
            this.quotaDiff = quotaDiff;
        }

        @VisibleForTesting
        public UserRateAggregate getRate() {
            return rate;
        }

        @VisibleForTesting
        public Long getRateDiff() {
            return rateDiff;
        }

        @VisibleForTesting
        public UserQuotaAggregate getQuota() {
            return quota;
        }

        @VisibleForTesting
        public Long getQuotaDiff() {
            return quotaDiff;
        }

        public static UserDiffs renew(UserDiffs old) {
            return new UserDiffs(old.rate, 0L, old.quota, 0L);
        }

        public static UserDiffs refresh(UserDiffs old, UserDiffs fresh) {
            return new UserDiffs(fresh.rate, old.rateDiff, fresh.quota, old.quotaDiff);
        }

        public static UserDiffs incrementQuotaAndRateDiffs(UserDiffs old) {
            return new UserDiffs(old.rate, old.rateDiff + 1, old.quota, old.quotaDiff + 1);
        }

        public static UserDiffs decrementRateDiff(UserDiffs old) {
            return new UserDiffs(old.rate, old.rateDiff - 1, old.quota, old.quotaDiff);
        }

        public Long getTotalQuota() {
            return quota.getCounter() + quotaDiff;
        }

        public Long getTotalRate() {
            return rate.getGauge() + rateDiff;
        }
    }

    public static class DiffSync {

        private final Long rateDiff;

        private final Long quotaDiff;

        public DiffSync(Long rateDiff, Long quotaDiff) {
            this.rateDiff = rateDiff;
            this.quotaDiff = quotaDiff;
        }

        @VisibleForTesting
        public Long getRateDiff() {
            return rateDiff;
        }


        @VisibleForTesting
        public Long getQuotaDiff() {
            return quotaDiff;
        }

        public static DiffSync combine(DiffSync left, DiffSync right) {
            return new DiffSync(
                left.rateDiff + right.rateDiff,
                left.quotaDiff + right.quotaDiff
            );
        }
    }
}
