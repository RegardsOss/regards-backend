package fr.cnes.regards.modules.storage.service.file.download;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserAction;
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
import io.vavr.collection.Seq;
import io.vavr.control.Either;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static fr.cnes.regards.modules.storage.dao.entity.download.DownloadQuotaLimitsEntity.UK_DOWNLOAD_QUOTA_LIMITS_EMAIL;
import static fr.cnes.regards.modules.storage.service.file.exception.DownloadLimitExceededException.buildDownloadQuotaExceededException;
import static fr.cnes.regards.modules.storage.service.file.exception.DownloadLimitExceededException.buildDownloadRateExceededException;

@Service
@MultitenantTransactional
public class DownloadQuotaService<T>
    implements IQuotaService<T>,ApplicationListener<ApplicationReadyEvent>, IBatchHandler<ProjectUserEvent> {

    private IDownloadQuotaRepository quotaRepository;

    private IQuotaManager quotaManager;

    private ITenantResolver tenantResolver;

    private IRuntimeTenantResolver runtimeTenantResolver;

    private ISubscriber subscriber;

    private ApplicationContext applicationContext;

    private DownloadQuotaService<T> self;

    private AtomicReference<Map<String, DefaultDownloadQuotaLimits>> defaultLimits;

    private Cache<QuotaKey, DownloadQuotaLimits> cache = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build();

    public DownloadQuotaService() {}

    @Autowired
    public DownloadQuotaService(
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
        self = applicationContext.getBean(DownloadQuotaService.class);

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

    public DefaultDownloadQuotaLimits initDefaultLimits() {
        return quotaRepository.getDefaultDownloadQuotaLimits();
    }

    @VisibleForTesting
    protected void setSelf(DownloadQuotaService<T> self) {
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
    public Try<List<DownloadQuotaLimitsDto>> getDownloadQuotaLimits(String[] userEmails) {
        return Arrays.stream(userEmails)
            .map(userEmail -> {
                QuotaKey key = QuotaKey.make(runtimeTenantResolver.getTenant(), userEmail);
                return cacheUserQuota(userEmail, key);
            })
            .map(quotaLimits -> quotaLimits
                .map(DownloadQuotaLimitsDto::fromDownloadQuotaLimits))
            .reduce(
                Either.<ListUserQuotaLimitsResultException, io.vavr.collection.List<DownloadQuotaLimitsDto>>right(io.vavr.collection.List.empty()),
                (e, t) -> t.isSuccess()
                    ? e.map(l -> l.append(t.get()))
                    : e.isRight() ? Either.left(ListUserQuotaLimitsResultException.make(t.getCause())) : e.mapLeft(err -> err.compose(ListUserQuotaLimitsResultException.make(t.getCause()))),
                (l, r) -> l
            )
            .map(io.vavr.collection.List::toJavaList)
            .toTry();
    }

    @Override
    public UserCurrentQuotas getCurrentQuotas(String userEmail) {

        QuotaKey key = QuotaKey.make(runtimeTenantResolver.getTenant(), userEmail);

        return cacheUserQuota(userEmail, key)
            .flatMap(quota ->
                Try.of(() -> quotaManager.get(quota))
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

    @Override
    public Try<List<UserCurrentQuotas>> getCurrentQuotas(String[] userEmails) {
        return Arrays.stream(userEmails)
            .map(userEmail -> Try.of(() -> getCurrentQuotas(userEmail)))
            .reduce(
                Either.<ListUserQuotaLimitsResultException, io.vavr.collection.List<UserCurrentQuotas>>right(io.vavr.collection.List.empty()),
                (e, t) -> t.isSuccess()
                    ? e.map(l -> l.append(t.get()))
                    : e.isRight() ? Either.left(ListUserQuotaLimitsResultException.make(t.getCause())) : e.mapLeft(err -> err.compose(ListUserQuotaLimitsResultException.make(t.getCause()))),
                (l, r) -> l
            )
            .map(io.vavr.collection.List::toJavaList)
            .toTry();
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
        return Try.of(() -> quotaManager.get(quotaLimits))
            .mapTry(quotaAndRate -> {
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

    public static abstract class ListUserQuotaLimitsResultException extends Exception {

        private ListUserQuotaLimitsResultException() { super(); }
        private ListUserQuotaLimitsResultException(Throwable cause) { super(cause); }

        public static final ListUserQuotaLimitsResultException EMPTY = new Empty();

        public static class Empty extends ListUserQuotaLimitsResultException {
            Empty() { super(); }
            @Override
            public Seq<Throwable> causes() { return io.vavr.collection.List.of(); }
        }

        public static class Single extends ListUserQuotaLimitsResultException {
            Single(Throwable cause) { super(cause == null ? new Exception() : cause); }
            @Override
            public Seq<Throwable> causes() {
                Throwable cause = getCause();
                return (cause instanceof ListUserQuotaLimitsResultException)
                    ? ((ListUserQuotaLimitsResultException)cause).causes()
                    : io.vavr.collection.List.of(cause);
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
                return causes.flatMap(cause -> (cause instanceof ListUserQuotaLimitsResultException)
                    ? ((ListUserQuotaLimitsResultException)cause).causes()
                    : io.vavr.collection.List.of(cause));
            }
        }

        public abstract Seq<Throwable> causes();

        public ListUserQuotaLimitsResultException compose(ListUserQuotaLimitsResultException other) {
            if (other == null) {
                return this;
            }
            return new Multiple(this.causes().appendAll(other.causes()));
        }

        public static ListUserQuotaLimitsResultException make(Throwable cause) { return new Single(cause); }
    }
}
