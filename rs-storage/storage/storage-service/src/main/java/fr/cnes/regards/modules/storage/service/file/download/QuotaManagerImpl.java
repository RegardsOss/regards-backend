package fr.cnes.regards.modules.storage.service.file.download;

import static fr.cnes.regards.modules.storage.service.file.download.QuotaConfiguration.QuotaManagerConfiguration.RATE_EXPIRATION_TICKING_SCHEDULER;
import static fr.cnes.regards.modules.storage.service.file.download.QuotaConfiguration.QuotaManagerConfiguration.SYNC_TICKING_SCHEDULER;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserQuotaAggregate;
import fr.cnes.regards.modules.storage.domain.database.UserRateAggregate;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import fr.cnes.regards.modules.storage.service.file.exception.DownloadLimitExceededException;
import io.vavr.Tuple;
import io.vavr.Tuple2;

@Component
@MultitenantTransactional
public class QuotaManagerImpl implements IQuotaManager {

    private long rateExpirationTick;

    private long syncTick;

    // We want to be sure to always have our own thread pool to maintain quota and rate coherence between all storage instances
    private ThreadPoolTaskScheduler rateExpirationTickingScheduler;

    private ThreadPoolTaskScheduler syncTickingScheduler;

    private IDownloadQuotaRepository quotaRepository;

    private ITenantResolver tenantResolver;

    private IRuntimeTenantResolver runtimeTenantResolver;

    private ApplicationContext applicationContext;

    private Environment env;

    private QuotaManagerImpl self;

    // not final because tests use the setter to assert
    private String instanceId = UUID.randomUUID().toString();

    // used to create a critical section around the sync logic
    // normally the sync should only be called from within the
    // `syncTickingScheduler` which is single threaded
    // but just in case... (equivalent to @Scheduled delay logic)
    private final AtomicBoolean inSync = new AtomicBoolean(false);

    // not final because tests use the setter to assert
    // not volatile because under normal conditions (i.e. not tests) this field is never reassigned
    private Map<String, Cache<String, UserDiffs>> userDiffsByTenant = new ConcurrentHashMap<>();

    // not final because tests use the setter to assert
    // not volatile because under normal conditions (i.e. not tests) this field is never reassigned
    // Accumulator used to continue normal instance operation while syncing with DB. At the end of sync, values in accumulator
    // are dumped into userDiffByTenant so things stay clean for the next sync attempt.
    private Map<String, Map<String, DiffSync>> diffsAccumulatorByTenant = new ConcurrentHashMap<>();

    public QuotaManagerImpl() {
    }

    @Autowired
    public QuotaManagerImpl(@Value("${regards.storage.rate.expiration.tick:120}") long rateExpirationTick,
            @Value("${regards.storage.quota.sync.tick:30}") long syncTick,
            @Qualifier(RATE_EXPIRATION_TICKING_SCHEDULER) ThreadPoolTaskScheduler rateExpirationTickingScheduler,
            @Qualifier(SYNC_TICKING_SCHEDULER) ThreadPoolTaskScheduler syncTickingScheduler,
            IDownloadQuotaRepository quotaRepository, ITenantResolver tenantResolver,
            IRuntimeTenantResolver runtimeTenantResolver, ApplicationContext applicationContext, Environment env) {
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

    @PostConstruct
    public void init() {
        self = applicationContext.getBean(QuotaManagerImpl.class);

        // init diffs and diffsAcc by tenant
        tenantResolver.getAllActiveTenants().forEach(tenant -> {
            getUserDiffsCache(tenant);
            diffsAccumulatorByTenant.put(tenant, new java.util.HashMap<>());
        });

        // start schedulers only if not in "noscheduler" profile (i.e. disable schedulers for tests)
        boolean shouldStartSchedulers = Arrays.stream(env.getActiveProfiles()).distinct() // maybe useless but I don't trust
                .noneMatch(profile -> profile.equals("noscheduler"));
        if (shouldStartSchedulers) {
            startGaugeExpirationScheduler();
            startDiffSyncScheduler();
        }
    }

    private void startGaugeExpirationScheduler() {
        rateExpirationTickingScheduler.setThreadNamePrefix("gauge-expiration-");
        rateExpirationTickingScheduler
                .scheduleWithFixedDelay(() -> tenantResolver.getAllActiveTenants().forEach(tenant -> {
                    runtimeTenantResolver.forceTenant(tenant);
                    try {
                        self.purgeExpiredGauges();
                    } finally {
                        runtimeTenantResolver.clearTenant();
                    }
                }), Duration.ofSeconds(rateExpirationTick));
    }

    private void startDiffSyncScheduler() {
        syncTickingScheduler.setThreadNamePrefix("quotas-sync-");
        syncTickingScheduler.scheduleWithFixedDelay(() -> tenantResolver.getAllActiveTenants().forEach(tenant -> {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                self.syncDiffs(tenant);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }), Duration.ofSeconds(syncTick));
    }

    // public just to let Spring do its proxy-thing without messing up my transactions
    public void purgeExpiredGauges() {
        quotaRepository.deleteExpiredRates();
    }

    // public just to let Spring do its proxy-thing without messing up my transactions
    public void syncDiffs(String tenant) {
        // create critical section, just in case because logic not thread-safe
        while (!inSync.compareAndSet(false, true)) {
            // wait until we get the lock
        }

        try {
            // sync will start, diffsAcc should be initialized if not found
            diffsAccumulatorByTenant.computeIfAbsent(tenant, t -> new java.util.HashMap<>());

            // swap current instance diff counters for tenant users to 0 and get previous value
            ConcurrentMap<String, UserDiffs> userDiffs = getUserDiffsCache(tenant).asMap();
            Map<String, UserDiffs> currentSync = new HashMap<>(); // used for the sake of legibility
            userDiffs.replaceAll((q, ud) -> {
                currentSync.put(q, ud);
                return UserDiffs.renew(ud);
            });
            // now get/incr/decr can continue working on a new userDiff => no contention with this method

            // accumulate quota/rate diffs in case sync fails
            Map<String, DiffSync> diffsAccByEmail = diffsAccumulatorByTenant.get(tenant);
            currentSync.forEach((email, currentUserDiff) -> {
                // 'r'(right) and 'l'(left) are convention usually used in functional programming as operands of merge operation (reduce, for example)
                // here r is current value to sync
                DiffSync r = new DiffSync(currentUserDiff.rateDiff, currentUserDiff.quotaDiff);
                // l is old value that could not be synced previously in accumulator (or 0 if current email has never been synced by this storage instance)
                diffsAccByEmail.compute(email, (ignored, l) -> (l == null) ? r : DiffSync.combine(l, r));
            });

            // do sync quota/rate with db
            Map<String, UserDiffs> newUserDiffs = self.flushSyncAndRefreshQuotas(diffsAccumulatorByTenant.get(tenant)//.get()
            );

            // swap user gauges for fresh global gauge but keep working instance counter diff
            newUserDiffs
                    .forEach((q, r) -> userDiffs.compute(q, (ignored, l) -> UserDiffs.refresh(l, r)));

            // sync is finished, diffsAcc for current tenant can be cleared for next sync
            diffsAccumulatorByTenant.put(tenant, new HashMap<>());
        } finally {
            // end critical section
            inSync.set(false);
        }
    }

    private Cache<String, UserDiffs> getUserDiffsCache(String tenant) {
        return userDiffsByTenant.computeIfAbsent(tenant, t -> Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES).maximumSize(10_000).build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, UserDiffs> flushSyncAndRefreshQuotas(Map<String, DiffSync> diffSyncs) {
        Map<String, UserDiffs> result = new HashMap<>();

        diffSyncs.forEach((email, sync) -> {
            // sync current instance quota/rate for user
            quotaRepository.upsertOrCombineDownloadQuota(instanceId, email, sync.quotaDiff);
            quotaRepository.upsertOrCombineDownloadRate(instanceId, email, sync.rateDiff,
                                                        LocalDateTime.now().plusSeconds(syncTick * 2));

            // and refresh its global quota/rate (select across all instances gauges)
            UserQuotaAggregate globalQuota = quotaRepository.fetchDownloadQuotaSum(email);
            UserRateAggregate globalRate = quotaRepository.fetchDownloadRatesSum(email);

            result.put(email, new UserDiffs(globalRate, 0L, globalQuota, 0L));
        });

        return result;
    }

    @Override
    public Tuple2<UserQuotaAggregate, UserRateAggregate> get(DownloadQuotaLimits quota) {

        String tenant = quota.getTenant();
        String email = quota.getEmail();

        runtimeTenantResolver.forceTenant(tenant);

        Cache<String, UserDiffs> gaugeSyncs = getUserDiffsCache(tenant);
        UserDiffs diffs = gaugeSyncs.get(email, ignored -> {
            // create its current instance quota/rate if not exist (diff = 0L so operation is idempotent and thus CAS-loop safe)
            quotaRepository.upsertOrCombineDownloadQuota(instanceId, email, 0L);
            quotaRepository.upsertOrCombineDownloadRate(instanceId, email, 0L,
                                                        LocalDateTime.now().plusSeconds(syncTick * 2));
            // and get its global quota/rate (select across all instances gauges)
            UserQuotaAggregate globalQuota = quotaRepository.fetchDownloadQuotaSum(email);
            UserRateAggregate globalRate = quotaRepository.fetchDownloadRatesSum(email);
            return new UserDiffs(globalRate, 0L, globalQuota, 0L);
        });

        // gaugeSyncs is a cache so diffs.quotaDiff and diffs.rateDiff might not be 0 as set during initialization(code right above)
        //noinspection ConstantConditions
        long counter = diffs.quota.getCounter() + diffs.quotaDiff;
        long gauge = diffs.rate.getGauge() + diffs.rateDiff;

        return Tuple.of(new UserQuotaAggregate(counter), new UserRateAggregate(gauge));
    }

    @Override
    public void increment(DownloadQuotaLimits quota) {
        String email = quota.getEmail();

        Cache<String, UserDiffs> userDiffs = getUserDiffsCache(quota.getTenant());
        if (userDiffs.getIfPresent(email) == null) {
            throw new IllegalStateException("Cannot incr before get");
        }
        userDiffs.asMap().computeIfPresent(email, (key, userDiff) -> {
            if ((userDiff.getTotalRate() >= quota.getRateLimit()) && (quota.getRateLimit() >= 0)) {
                // nice try little thread, but no, you're too late
                throw DownloadLimitExceededException.buildDownloadRateExceededException(email, quota.getRateLimit(),
                                                                                        userDiff.getTotalRate());
            }
            return UserDiffs.incrementQuotaAndRateDiffs(userDiff);
        });
    }

    @Override
    public void decrement(DownloadQuotaLimits quota) {
        String email = quota.getEmail();

        Cache<String, UserDiffs> userDiffs = getUserDiffsCache(quota.getTenant());
        if (userDiffs.getIfPresent(email) == null) {
            throw new IllegalStateException("Cannot decr before get");
        }
        userDiffs.asMap().computeIfPresent(email, (q, u) -> UserDiffs.decrementRateDiff(u));
    }

    @VisibleForTesting
    protected void setSelf(QuotaManagerImpl self) {
        this.self = self;
    }

    @VisibleForTesting
    public void setUserDiffsByTenant(Map<String, Cache<String, UserDiffs>> userDiffsByTenant) {
        this.userDiffsByTenant = userDiffsByTenant;
    }

    @VisibleForTesting
    public void setDiffsAccumulatorByTenant(Map<String, Map<String, DiffSync>> diffsAccumulator) {
        this.diffsAccumulatorByTenant = diffsAccumulator;
    }

    @VisibleForTesting
    public Map<String, Map<String, DiffSync>> getDiffsAccumulatorByTenant() {
        return diffsAccumulatorByTenant;
    }

    @VisibleForTesting
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
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
            return new UserDiffs(new UserRateAggregate(old.rate.getGauge() + old.rateDiff), 0L,
                    new UserQuotaAggregate(old.quota.getCounter() + old.quotaDiff), 0L);
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
            return new DiffSync(left.rateDiff + right.rateDiff, left.quotaDiff + right.quotaDiff);
        }
    }
}
