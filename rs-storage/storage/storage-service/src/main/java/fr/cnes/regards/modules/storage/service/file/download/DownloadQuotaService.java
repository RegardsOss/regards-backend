package fr.cnes.regards.modules.storage.service.file.download;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSettingService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserAction;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserEvent;
import static fr.cnes.regards.modules.storage.dao.entity.download.DownloadQuotaLimitsEntity.UK_DOWNLOAD_QUOTA_LIMITS_EMAIL;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.domain.database.DefaultDownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import static fr.cnes.regards.modules.storage.service.file.exception.DownloadLimitExceededException.buildDownloadQuotaExceededException;
import static fr.cnes.regards.modules.storage.service.file.exception.DownloadLimitExceededException.buildDownloadRateExceededException;
import fr.cnes.regards.modules.storage.service.settings.StorageSettingService;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

@Service
@MultitenantTransactional
public class DownloadQuotaService<T> implements IQuotaService<T>, IBatchHandler<ProjectUserEvent> {

    public static abstract class ListUserQuotaLimitsResultException extends Exception {

        public static class Empty extends ListUserQuotaLimitsResultException {

            Empty() {
                super();
            }

            @Override
            public Seq<Throwable> causes() {
                return io.vavr.collection.List.of();
            }
        }

        public static class Single extends ListUserQuotaLimitsResultException {

            Single(Throwable cause) {
                super(cause == null ? new Exception() : cause);
            }

            @Override
            public Seq<Throwable> causes() {
                Throwable cause = getCause();
                return (cause instanceof ListUserQuotaLimitsResultException) ?
                        ((ListUserQuotaLimitsResultException) cause).causes() :
                        io.vavr.collection.List.of(cause);
            }
        }

        public static class Multiple extends ListUserQuotaLimitsResultException {

            private final Seq<Throwable> causes;

            Multiple(Seq<Throwable> causes) {
                super();
                this.causes = Option.of(causes).getOrElse(io.vavr.collection.List.empty());
            }

            @Override
            public Seq<Throwable> causes() {
                return causes.flatMap(cause -> (cause instanceof ListUserQuotaLimitsResultException) ?
                        ((ListUserQuotaLimitsResultException) cause).causes() :
                        io.vavr.collection.List.of(cause));
            }
        }

        public static final ListUserQuotaLimitsResultException EMPTY = new Empty();

        private ListUserQuotaLimitsResultException() {
            super();
        }

        private ListUserQuotaLimitsResultException(Throwable cause) {
            super(cause);
        }

        public static ListUserQuotaLimitsResultException make(Throwable cause) {
            return new Single(cause);
        }

        public abstract Seq<Throwable> causes();

        public ListUserQuotaLimitsResultException compose(ListUserQuotaLimitsResultException other) {
            if (other == null) {
                return this;
            }
            return new Multiple(this.causes().appendAll(other.causes()));
        }
    }

    private IDownloadQuotaRepository quotaRepository;

    private IQuotaManager quotaManager;

    private ITenantResolver tenantResolver;

    private IRuntimeTenantResolver runtimeTenantResolver;

    private ISubscriber subscriber;

    private IDynamicTenantSettingService dynamicTenantSettingService;

    private ApplicationContext applicationContext;

    private DownloadQuotaService<T> self;

    private AtomicReference<Map<String, DefaultDownloadQuotaLimits>> defaultLimits = new AtomicReference<>(HashMap.empty());

    private Cache<QuotaKey, DownloadQuotaLimits> cache = Caffeine.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES)
            .maximumSize(10_000).build();

    private StorageSettingService storageSettingService;

    public DownloadQuotaService() {
    }

    @Autowired
    public DownloadQuotaService(IDownloadQuotaRepository quotaRepository, IQuotaManager quotaManager,
            ITenantResolver tenantResolver, IRuntimeTenantResolver runtimeTenantResolver, ISubscriber subscriber,
            ApplicationContext applicationContext, IDynamicTenantSettingService dynamicTenantSettingService, StorageSettingService storageSettingService) {
        this.quotaRepository = quotaRepository;
        this.quotaManager = quotaManager;
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.subscriber = subscriber;
        this.applicationContext = applicationContext;
        this.dynamicTenantSettingService = dynamicTenantSettingService;
        this.storageSettingService = storageSettingService;
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        self = applicationContext.getBean(DownloadQuotaService.class);
        subscriber.subscribeTo(ProjectUserEvent.class, this);
        for(String tenant: tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                for (DynamicTenantSetting setting : storageSettingService.retrieve()) {
                    if (Objects.equals(StorageSetting.MAX_QUOTA_NAME, setting.getName())) {
                        changeDefaultQuotaLimits(setting.getValue());
                    }
                    if (Objects.equals(StorageSetting.RATE_LIMIT_NAME, setting.getName())) {
                        changeDefaultRateLimits(setting.getValue());
                    }
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @VisibleForTesting
    protected void setSelf(DownloadQuotaService<T> self) {
        this.self = self;
    }

    @VisibleForTesting
    public void setCache(Cache<QuotaKey, DownloadQuotaLimits> cache) {
        this.cache = cache;
    }

    @Override
    public boolean validate(String tenant, ProjectUserEvent message) {
        return true;
    }

    @Override
    @MultitenantTransactional(propagation = Propagation.NOT_SUPPORTED)
    public void handleBatch(String tenant, List<ProjectUserEvent> messages) {
        runtimeTenantResolver.forceTenant(tenant);
        runtimeTenantResolver.forceTenant(tenant);
        Set<String> emailsToRemove = new HashSet<>();
        messages.forEach(event -> {
            if (event.getAction() == ProjectUserAction.DELETION) {
                emailsToRemove.add(event.getEmail());
            }
        });
        if (!emailsToRemove.isEmpty()) {
            self.removeQuotaFor(emailsToRemove);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Try<T> withQuota(String userEmail, Function<WithQuotaOperationHandler, Try<T>> operation) {

        QuotaKey key = QuotaKey.make(runtimeTenantResolver.getTenant(), userEmail);

        // flatMap is only called in case try is still in success
        //t._1 represent current quota and rate limit for this instance
        return cacheUserQuota(userEmail, key).flatMap(self::getUserQuotaAndRate).flatMap(t -> apply(operation, t._1));
    }

    @Override
    public void changeDefaultQuotaLimits(Long newDefaultQuota) {
        Option<DefaultDownloadQuotaLimits> oldDefault = defaultLimits.get().get(runtimeTenantResolver.getTenant());
        if (oldDefault.isEmpty()) {
            defaultLimits.updateAndGet(m -> m.put(runtimeTenantResolver.getTenant(),
                                                  new DefaultDownloadQuotaLimits(newDefaultQuota,
                                                                                 StorageSetting.RATE_LIMIT
                                                                                         .getDefaultValue())));
        } else {
            defaultLimits.updateAndGet(m -> m.put(runtimeTenantResolver.getTenant(),
                                                  new DefaultDownloadQuotaLimits(newDefaultQuota,
                                                                                 oldDefault.get().getRateLimit())));
        }
    }

    @Override
    public void changeDefaultRateLimits(Long newDefaultRate) {
        Option<DefaultDownloadQuotaLimits> oldDefault = defaultLimits.get().get(runtimeTenantResolver.getTenant());
        if (oldDefault.isEmpty()) {
            defaultLimits.updateAndGet(m -> m.put(runtimeTenantResolver.getTenant(),
                                                  new DefaultDownloadQuotaLimits(StorageSetting.MAX_QUOTA
                                                                                         .getDefaultValue(),
                                                                                 newDefaultRate)));
        } else {
            defaultLimits.updateAndGet(m -> m.put(runtimeTenantResolver.getTenant(),
                                                  new DefaultDownloadQuotaLimits(oldDefault.get().getMaxQuota(),
                                                                                 newDefaultRate)));
        }
    }

    @Override
    public Try<DownloadQuotaLimitsDto> upsertDownloadQuotaLimits(DownloadQuotaLimitsDto newLimits) {
        return Option.ofOptional(quotaRepository.findByEmail(newLimits.getEmail()))
                .map(limits -> new DownloadQuotaLimits(limits.getId(),
                                                       limits.getTenant(),
                                                       limits.getEmail(),
                                                       newLimits.getMaxQuota(),
                                                       newLimits.getRateLimit()))
                .orElse(Option.of(new DownloadQuotaLimits(runtimeTenantResolver.getTenant(),
                                                          newLimits.getEmail(),
                                                          newLimits.getMaxQuota(),
                                                          newLimits.getRateLimit()))).map(quotaRepository::save).toTry()
                .peek(limits -> {
                    QuotaKey key = QuotaKey.make(runtimeTenantResolver.getTenant(), newLimits.getEmail());
                    cache.put(key, limits);
                }).map(DownloadQuotaLimitsDto::fromDownloadQuotaLimits);
    }

    @Override
    public Try<DownloadQuotaLimitsDto> getDownloadQuotaLimits(String userEmail) {
        QuotaKey key = QuotaKey.make(runtimeTenantResolver.getTenant(), userEmail);
        return cacheUserQuota(userEmail, key).map(DownloadQuotaLimitsDto::fromDownloadQuotaLimits);
    }

    @Override
    public Try<List<DownloadQuotaLimitsDto>> getDownloadQuotaLimits(String[] userEmails) {
        return Arrays.stream(userEmails).map(userEmail -> {
            QuotaKey key = QuotaKey.make(runtimeTenantResolver.getTenant(), userEmail);
            return cacheUserQuota(userEmail, key);
        }).map(quotaLimits -> quotaLimits.map(DownloadQuotaLimitsDto::fromDownloadQuotaLimits))
                .reduce(Either.<ListUserQuotaLimitsResultException, io.vavr.collection.List<DownloadQuotaLimitsDto>>right(
                        io.vavr.collection.List.empty()),
                        (e, t) -> t.isSuccess() ?
                                e.map(l -> l.append(t.get())) :
                                e.isRight() ?
                                        Either.left(ListUserQuotaLimitsResultException.make(t.getCause())) :
                                        e.mapLeft(err -> err
                                                .compose(ListUserQuotaLimitsResultException.make(t.getCause()))),
                        (l, r) -> l).map(io.vavr.collection.List::toJavaList).toTry();
    }

    @Override
    public UserCurrentQuotas getCurrentQuotas(String userEmail) {

        QuotaKey key = QuotaKey.make(runtimeTenantResolver.getTenant(), userEmail);

        return cacheUserQuota(userEmail, key).flatMap(quota -> Try.of(() -> quotaManager.get(quota))
                .map(quotaAndRate -> new UserCurrentQuotas(userEmail,
                                                           quota.getMaxQuota(),
                                                           quota.getRateLimit(),
                                                           quotaAndRate._1.getCounter(),
                                                           quotaAndRate._2.getGauge()))).get();
    }

    @Override
    public Try<List<UserCurrentQuotas>> getCurrentQuotas(String[] userEmails) {
        return Arrays.stream(userEmails).map(userEmail -> Try.of(() -> getCurrentQuotas(userEmail)))
                .reduce(Either.<ListUserQuotaLimitsResultException, io.vavr.collection.List<UserCurrentQuotas>>right(io.vavr.collection.List
                                                                                                                             .empty()),
                        (e, t) -> t.isSuccess() ?
                                e.map(l -> l.append(t.get())) :
                                e.isRight() ?
                                        Either.left(ListUserQuotaLimitsResultException.make(t.getCause())) :
                                        e.mapLeft(err -> err
                                                .compose(ListUserQuotaLimitsResultException.make(t.getCause()))),
                        (l, r) -> l).map(io.vavr.collection.List::toJavaList).toTry();
    }

    @VisibleForTesting
    protected Try<DownloadQuotaLimits> cacheUserQuota(String userEmail, QuotaKey key) {
        DefaultDownloadQuotaLimits defaults = getDefaultLimits();
        return Try.of(() -> cache.get(key,
                                      __ -> self.findOrCreateDownloadQuota(userEmail,
                                                                           defaults.getMaxQuota(),
                                                                           defaults.getRateLimit())));
    }

    /**
     * In caller logic, we just got the quota and rate for this user but nothing prevents other thread from this instance to have modified them.
     * For example, this user has just finished downloading a file (started before we looked for quota and rate in this thread)
     * so its rate and quota have been modified and we are looking for the most recent values possible
     * @param quotaLimits retrieved by this thread
     * @return If success: current quota and rate value for this instance(thanks to quotaManager) and previously retrieved values.
     * If quota or rate exceeded: try containing exceptions
     */
    @VisibleForTesting
    protected Try<Tuple3<DownloadQuotaLimits, Long, Long>> getUserQuotaAndRate(DownloadQuotaLimits quotaLimits) {
        return Try.of(() -> quotaManager.get(quotaLimits)).mapTry(quotaAndRate -> {
            Long quota = quotaAndRate._1.getCounter();
            if (quota >= quotaLimits.getMaxQuota() && quotaLimits.getMaxQuota() >= 0) {
                throw buildDownloadQuotaExceededException(quotaLimits.getEmail(), quotaLimits.getMaxQuota(), quota);
            }
            Long rate = quotaAndRate._2.getGauge();
            if (rate >= quotaLimits.getRateLimit() && quotaLimits.getRateLimit() >= 0) {
                throw buildDownloadRateExceededException(quotaLimits.getEmail(), quotaLimits.getRateLimit(), rate);
            }
            return Tuple.of(quotaLimits, quota, rate);
        });
    }

    @Override
    public void removeQuotaFor(Set<String> emails) {
        String tenant = runtimeTenantResolver.getTenant();
        emails.forEach(email -> {
            QuotaKey key = QuotaKey.make(tenant, email);
            quotaRepository.deleteByEmail(email);
            cache.invalidate(key);
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
        // an operation which is asynchronous and where the caller
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
        return Option.ofOptional(quotaRepository.findByEmail(userEmail)).getOrElseTry(() -> {
            try {
                return self.createDownloadQuota(userEmail, maxQuota, rateLimit);
            } catch (DataIntegrityViolationException e) {
                //This case means that we could not create the quota for this user in DB because another process (from storage)
                // has done it for use while we were trying to see if it already existed. So lets simply get the one created for us
                return Option.of(e.getRootCause()).filter(t -> t instanceof PSQLException)
                        .filter(t -> t.getMessage().contains(UK_DOWNLOAD_QUOTA_LIMITS_EMAIL))
                        .flatMap(throwable -> Option.ofOptional(quotaRepository.findByEmail(userEmail)))
                        .getOrElseThrow(() -> e);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DownloadQuotaLimits createDownloadQuota(String userEmail, Long maxQuota, Long rateLimit) {
        return quotaRepository
                .save(new DownloadQuotaLimits(runtimeTenantResolver.getTenant(), userEmail, maxQuota, rateLimit));
    }

    private DefaultDownloadQuotaLimits getDefaultLimits() {
        // default limit for a new tenant should have been initialized by DynamicTenantSetting logic handling new tenant creation
        return defaultLimits.get().get(runtimeTenantResolver.getTenant()).get();
    }

    @VisibleForTesting
    public void setDefaultLimits(AtomicReference<Map<String, DefaultDownloadQuotaLimits>> defaultLimits) {
        this.defaultLimits = defaultLimits;
    }
}
