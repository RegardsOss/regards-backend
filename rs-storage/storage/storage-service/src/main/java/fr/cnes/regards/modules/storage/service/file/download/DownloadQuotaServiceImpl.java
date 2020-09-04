package fr.cnes.regards.modules.storage.service.file.download;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserEvent;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.function.Function;

import static fr.cnes.regards.modules.storage.dao.entity.download.DownloadQuotaLimitsEntity.UK_DOWNLOAD_QUOTA_LIMITS_EMAIL;
import static fr.cnes.regards.modules.storage.service.file.exception.DownloadQuotaLimitExceededException.buildDownloadQuotaExceededException;
import static fr.cnes.regards.modules.storage.service.file.exception.DownloadQuotaLimitExceededException.buildDownloadRateExceededException;

@Service
@MultitenantTransactional
public class DownloadQuotaServiceImpl<T>
    implements IQuotaService<T>,ApplicationListener<ApplicationReadyEvent>, IBatchHandler<ProjectUserEvent> {

    private long defaultQuota;

    private long defaultRate;

    private IDownloadQuotaRepository quotaRepository;

    private IQuotaManager quotaManager;

    private IRuntimeTenantResolver tenantResolver;

    private ISubscriber subscriber;

    private ApplicationContext applicationContext;

    private DownloadQuotaServiceImpl<T> self;

    private Cache<QuotaKey, DownloadQuotaLimits> cache = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build();

    public DownloadQuotaServiceImpl() {}

    @Autowired
    public DownloadQuotaServiceImpl(
        @Value("${regards.admin.quota.max.default:-1}") long defaultQuota,
        @Value("${regards.admin.rate.limit.default:-1}") long defaultRate,
        IDownloadQuotaRepository quotaRepository,
        IQuotaManager quotaManager,
        IRuntimeTenantResolver tenantResolver,
        ISubscriber subscriber,
        ApplicationContext applicationContext
    ) {
        this.defaultQuota = defaultQuota;
        this.defaultRate = defaultRate;
        this.quotaRepository = quotaRepository;
        this.quotaManager = quotaManager;
        this.tenantResolver = tenantResolver;
        this.subscriber = subscriber;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        self = applicationContext.getBean(DownloadQuotaServiceImpl.class);
    }

    @VisibleForTesting
    protected void setSelf(DownloadQuotaServiceImpl<T> self) {
        this.self = self;
    }

    @VisibleForTesting
    protected void setCache(Cache<QuotaKey, DownloadQuotaLimits> cache) {
        this.cache = cache;
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
        tenantResolver.forceTenant(tenant);
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
    public UserCurrentQuotas getCurrentQuotas(String userEmail) {

        QuotaKey key = QuotaKey.make(tenantResolver.getTenant(), userEmail);

        return cacheUserQuota(userEmail, key)
            .flatMap(quota ->
                Try.of(() -> quotaManager.get(quota).get())
                    .map(quotaAndRate ->
                        new UserCurrentQuotas(
                            tenantResolver.getTenant(),
                            userEmail,
                            quota.getMaxQuota(),
                            quota.getRateLimit(),
                            quotaAndRate._1.getCounter(),
                            quotaAndRate._2.getGauge()
                        )
                    )
            ).get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Try<T> withQuota(String userEmail, Function<WithQuotaOperationHandler, Try<T>> operation) {

        QuotaKey key = QuotaKey.make(tenantResolver.getTenant(), userEmail);

        return cacheUserQuota(userEmail, key)
            .flatMap(self::getUserQuotaAndRate)
            .flatMap(t -> apply(operation, t._1));
    }

    @VisibleForTesting
    protected Try<DownloadQuotaLimits> cacheUserQuota(String userEmail, QuotaKey key) {
        return Try.of(() ->
            cache.get(key, __ -> self.findOrCreateDownloadQuota(userEmail))
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
    public DownloadQuotaLimits findOrCreateDownloadQuota(String userEmail) {
        return Option.ofOptional(quotaRepository.findByEmail(userEmail))
            .getOrElseTry(() -> {
                try {
                    return self.createDefaultDownloadQuota(userEmail);
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
    public DownloadQuotaLimits createDefaultDownloadQuota(String userEmail) {
        return quotaRepository.save(new DownloadQuotaLimits(tenantResolver.getTenant(), userEmail, defaultQuota, defaultRate));
    }
}
