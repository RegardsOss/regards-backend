package fr.cnes.regards.modules.storage.service.file.download;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserEvent;
import fr.cnes.regards.modules.storage.domain.database.DefaultDownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static fr.cnes.regards.modules.storage.dao.entity.download.DownloadQuotaLimitsEntity.UK_DOWNLOAD_QUOTA_LIMITS_EMAIL;
import static fr.cnes.regards.modules.storage.service.file.exception.DownloadQuotaLimitExceededException.buildDownloadQuotaExceededException;
import static fr.cnes.regards.modules.storage.service.file.exception.DownloadQuotaLimitExceededException.buildDownloadRateExceededException;

@Service
@MultitenantTransactional
public class DownloadQuotaServiceImpl<T>
    implements IQuotaService<T>,ApplicationListener<ApplicationReadyEvent>, IBatchHandler<ProjectUserEvent> {

    private IDownloadQuotaRepository quotaRepository;

    private IQuotaManager quotaManager;

    private ITenantResolver tenantResolver;

    private IRuntimeTenantResolver runtimeTenantResolver;

    private ISubscriber subscriber;

    private ApplicationContext applicationContext;

    private DownloadQuotaServiceImpl<T> self;

    private AtomicReference<Map<String, DefaultDownloadQuotaLimits>> defaultLimits;

    private Cache<QuotaKey, DownloadQuotaLimits> cache = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build();

    public DownloadQuotaServiceImpl() {}

    @Autowired
    public DownloadQuotaServiceImpl(
        IDownloadQuotaRepository quotaRepository,
        IQuotaManager quotaManager,
        ITenantResolver tenantResolver,
        IRuntimeTenantResolver runtimeTenantResolver,
        ISubscriber subscriber,
        ApplicationContext applicationContext
    ) {
        this.quotaRepository = quotaRepository;
        this.quotaManager = quotaManager;
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.subscriber = subscriber;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        self = applicationContext.getBean(DownloadQuotaServiceImpl.class);

//        self.initDefaultLimits();
        defaultLimits = new AtomicReference<>(
            tenantResolver.getAllActiveTenants()
                .stream()
                .reduce(
                    HashMap.empty(),
                    (m, tenant) -> {
                        runtimeTenantResolver.forceTenant(tenant);
                        m = m.put(tenant, self.initDefaultLimits()); //quotaRepository.getDefaultDownloadQuotaLimits());
                        runtimeTenantResolver.clearTenant();
                        return m;
                    },
                    (l, r) -> l)
        );
    }

    //@Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DefaultDownloadQuotaLimits initDefaultLimits() {
        return quotaRepository.getDefaultDownloadQuotaLimits();
//        java.util.Map<String, DefaultDownloadQuotaLimits> m = new java.util.HashMap<>();
//        tenantResolver.getAllActiveTenants()
//            .forEach(tenant -> {
//                runtimeTenantResolver.forceTenant(tenant);
//                m.put(tenant, quotaRepository.getDefaultDownloadQuotaLimits());
//                runtimeTenantResolver.clearTenant();
//            });
//        defaultLimits = new AtomicReference<>(HashMap.ofAll(m));
    }

    @VisibleForTesting
    protected void setSelf(DownloadQuotaServiceImpl<T> self) {
        this.self = self;
    }

    @VisibleForTesting
    public void setCache(Cache<QuotaKey, DownloadQuotaLimits> cache) {
        this.cache = cache;
    }

    @VisibleForTesting
    public void setDefaultLimits(AtomicReference<Map<String, DefaultDownloadQuotaLimits>> defaultLimits) {
        this.defaultLimits = defaultLimits;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(ProjectUserEvent.class, this);
    }

    @Override
    public boolean validate(String tenant, ProjectUserEvent message) {
        return true;
    }

    @Override
    public void handleBatch(String tenant, List<ProjectUserEvent> messages) {
        runtimeTenantResolver.forceTenant(tenant);
        messages.forEach(event ->  {
            QuotaKey key = QuotaKey.make(tenant, event.getEmail());
            switch (event.getAction()) {
                case DELETION:
                    quotaRepository.deleteByEmail(event.getEmail());
                    cache.invalidate(key);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Try<T> withQuota(String userEmail, Function<WithQuotaOperationHandler, Try<T>> operation) {

        QuotaKey key = QuotaKey.make(runtimeTenantResolver.getTenant(), userEmail);

        return cacheUserQuota(userEmail, key)
            .flatMap(self::getUserQuotaAndRate)
            .flatMap(t -> apply(operation, t._1));
    }

    @Override
    public Try<DefaultDownloadQuotaLimits> getDefaultDownloadQuotaLimits() {
        return Try.of(() ->
            quotaRepository.getDefaultDownloadQuotaLimits());
    }

    @Override
    public Try<DefaultDownloadQuotaLimits> changeDefaultDownloadQuotaLimits(DefaultDownloadQuotaLimits newDefaults) {
        return Try.of(() ->
            self.changeDefaultDownloadQuotaLimits(newDefaults.getMaxQuota(), newDefaults.getRateLimit()))
            .peek(d -> defaultLimits.updateAndGet(m ->
                m.put(runtimeTenantResolver.getTenant(), d)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DefaultDownloadQuotaLimits changeDefaultDownloadQuotaLimits(Long maxQuota, Long rateLimit) {
        return quotaRepository.changeDefaultDownloadQuotaLimits(maxQuota, rateLimit);
    }

    @Override
    public Try<DownloadQuotaLimitsDto> upsertDownloadQuotaLimits(DownloadQuotaLimitsDto newLimits) {
        return Option.ofOptional(quotaRepository.findByEmail(newLimits.getEmail()))
            .map(limits -> new DownloadQuotaLimits(
                limits.getId(),
                limits.getTenant(),
                limits.getEmail(),
                newLimits.getMaxQuota(),
                newLimits.getRateLimit()
            ))
            .orElse(Option.of(new DownloadQuotaLimits(
                runtimeTenantResolver.getTenant(),
                newLimits.getEmail(),
                newLimits.getMaxQuota(),
                newLimits.getRateLimit()
                ))
            ).map(quotaRepository::save)
            .toTry()
            .peek(limits -> {
                QuotaKey key = QuotaKey.make(runtimeTenantResolver.getTenant(), newLimits.getEmail());
                cache.put(key, limits);
            })
            .map(DownloadQuotaLimitsDto::fromDownloadQuotaLimits);
    }

    @Override
    public Try<DownloadQuotaLimitsDto> getDownloadQuotaLimits(String userEmail) {
        QuotaKey key = QuotaKey.make(runtimeTenantResolver.getTenant(), userEmail);
        return cacheUserQuota(userEmail, key)
            .map(DownloadQuotaLimitsDto::fromDownloadQuotaLimits);
    }

    @Override
    public UserCurrentQuotas getCurrentQuotas(String userEmail) {

        QuotaKey key = QuotaKey.make(runtimeTenantResolver.getTenant(), userEmail);

        return cacheUserQuota(userEmail, key)
            .flatMap(quota ->
                Try.of(() -> quotaManager.get(quota).get())
                    .map(quotaAndRate ->
                        new UserCurrentQuotas(
                            userEmail,
                            quota.getMaxQuota(),
                            quota.getRateLimit(),
                            quotaAndRate._1.getCounter(),
                            quotaAndRate._2.getGauge()
                        )
                    )
            ).get();
    }

    @VisibleForTesting
    protected Try<DownloadQuotaLimits> cacheUserQuota(String userEmail, QuotaKey key) {
        DefaultDownloadQuotaLimits defaults = getDefaultLimits();
        return Try.of(() ->
            cache.get(key, __ -> self.findOrCreateDownloadQuota(userEmail, defaults.getMaxQuota(), defaults.getRateLimit()))
        );
    }

    @VisibleForTesting
    protected Try<Tuple3<DownloadQuotaLimits, Long, Long>> getUserQuotaAndRate(DownloadQuotaLimits quotaLimits) {
        return Try.of(() -> quotaManager.get(quotaLimits).get())
            .mapTry(quotaAndRate -> {
                Long quota = quotaAndRate._1.getCounter();
                if (quota >= quotaLimits.getMaxQuota() && quotaLimits.getMaxQuota() >= 0) {
                    throw buildDownloadQuotaExceededException(quotaLimits.getEmail(), quotaLimits.getMaxQuota(), quota);
                }
                Long rate = quotaAndRate._2.getGauge();
                if (rate >= quotaLimits.getRateLimit() && quotaLimits.getRateLimit() >= 0) {
                    throw buildDownloadRateExceededException(quotaLimits.getEmail(), quotaLimits.getMaxQuota(), rate);
                }
                return Tuple.of(quotaLimits, quota, rate);
            });
    }

    @VisibleForTesting
    protected Try<T> apply(Function<WithQuotaOperationHandler, Try<T>> operation, DownloadQuotaLimits quota) {
        // WARNING: don't try to auto-magically handle an operation failure here.
        //
        // While it is tempting to think that a leaking exception could
        // be caught with a try/catch block around this block and that
        // an unbalanced counter could be restored automatically,
        // with code like this for instance :
        //
        // AtomicReference<WithQuotaOperationHandler> danglingQuota = new AtomicReference<>();
        // try {
        //     return operation.apply(new WithQuotaOperationHandler() {
        //         @Override
        //         public void start() {
        //             quotaManager.increment(key);
        //             danglingQuota.set(this);
        //         }
        //
        //         @Override
        //         public void stop() {
        //             quotaManager.decrement(key);
        //             danglingQuota.set(null);
        //         }
        //     });
        // }
        // finally {
        //     if (danglingQuota.get()!=null) {
        //         danglingQuota.get().stop();
        //     }
        // }
        //
        // Please refrain from doing so because it would conflict with
        // and operation which is asynchronous and where the caller
        // simply decided to keep a reference to the quotaHandler and
        // call stop in a callback of its own.
        //
        // Code like above would actually introduce an unbalanced decrement
        // in such case.
        //
        // It's a bit of a contrived example, but it illustrates that
        // trying to be too clever could actually make the quota manager
        // abstraction leak and cause issues to the called.
        return operation.apply(new WithQuotaOperationHandler() {
            @Override
            public void start() {
                quotaManager.increment(quota);
            }

            @Override
            public void stop() {
                quotaManager.decrement(quota);
            }
        });
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public DownloadQuotaLimits findOrCreateDownloadQuota(String userEmail, Long maxQuota, Long rateLimit) {
        return Option.ofOptional(quotaRepository.findByEmail(userEmail))
            .getOrElseTry(() -> {
                try {
                    return self.createDownloadQuota(userEmail, maxQuota, rateLimit);
                } catch (DataIntegrityViolationException e) {
                    return Option.of(e.getRootCause())
                        .filter(t -> t instanceof PSQLException)
                        .filter(t -> t.getMessage().contains(UK_DOWNLOAD_QUOTA_LIMITS_EMAIL))
                        .flatMap(throwable -> Option.ofOptional(quotaRepository.findByEmail(userEmail)))
                        .getOrElseThrow(() -> e);
                }
            });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DownloadQuotaLimits createDownloadQuota(String userEmail, Long maxQuota, Long rateLimit) {
        return quotaRepository.save(
            new DownloadQuotaLimits(
                runtimeTenantResolver.getTenant(),
                userEmail,
                maxQuota,
                rateLimit
            )
        );
    }

    private DefaultDownloadQuotaLimits getDefaultLimits() {
        return defaultLimits.get().get(runtimeTenantResolver.getTenant()).get();
    }
}
