package fr.cnes.regards.modules.storage.service.file.download;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.modules.storage.domain.DownloadableFile;
import fr.cnes.regards.modules.storage.service.file.exception.DownloadLimitExceededException;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static fr.cnes.regards.modules.storage.service.file.download.QuotaConfiguration.QuotaExceededReporterConfiguration.REPORT_TICKING_SCHEDULER;

@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DownloadQuotaExceededReporter implements IQuotaExceededReporter<DownloadableFile>, InitializingBean {

    public static final String TITLE = "Download quota errors";

    public static final int ELLIPSIS_THRESHOLD = 3;
    public static final String MORE_DOWNLOAD_ERRORS_TEMPLATE = "\n... and %d more download errors.";

    private long reportTick;

    private ThreadPoolTaskScheduler reportTickingScheduler;

    private INotificationClient notificationClient;

    private ITenantResolver tenantResolver;

    private IRuntimeTenantResolver runtimeTenantResolver;

    private ApplicationContext applicationContext;

    private Environment env;

    private DownloadQuotaExceededReporter self;

    // Map<(tenant, user), (failure causes written, more causes but not written)>
    // FIXME: change to Map<tenant, Map<user, (failure causes written, more causes but not written)>> for legibility
    private AtomicReference<Map<QuotaKey, Tuple2<List<String>, Long>>> errors = new AtomicReference<>(HashMap.empty());

    public DownloadQuotaExceededReporter() {}

    @Autowired
    public DownloadQuotaExceededReporter(
        @Value("${regards.storage.quota.report.tick:30}") long reportTick,
        @Qualifier(REPORT_TICKING_SCHEDULER) ThreadPoolTaskScheduler reportTickingScheduler,
        INotificationClient notificationClient,
        ITenantResolver tenantResolver,
        IRuntimeTenantResolver runtimeTenantResolver,
        ApplicationContext applicationContext,
        Environment env,
        DownloadQuotaExceededReporter downloadQuotaExceededReporter
    ) {
        this.reportTick = reportTick;
        this.reportTickingScheduler = reportTickingScheduler;
        this.notificationClient = notificationClient;
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.applicationContext = applicationContext;
        this.env = env;
        this.self = downloadQuotaExceededReporter;
    }

    @VisibleForTesting
    public void setSelf(DownloadQuotaExceededReporter self) {
        this.self = self;
    }

    @VisibleForTesting
    public void setErrors(AtomicReference<Map<QuotaKey, Tuple2<List<String>, Long>>> errors) {
        this.errors = errors;
    }

    @Override
    public void afterPropertiesSet() {
        self = applicationContext.getBean(DownloadQuotaExceededReporter.class);

        // start schedulers only if not in "noscheduler" profile (i.e. disable schedulers for tests)
        boolean shouldStartSchedulers =
            Arrays.stream(env.getActiveProfiles())
                .distinct() // maybe useless but I don't trust
                .noneMatch(profile -> profile.equals("noscheduler"));
        if (shouldStartSchedulers) {
            startReportSyncScheduler();
        }
    }

    private void startReportSyncScheduler() {
        reportTickingScheduler.setThreadNamePrefix("quotas-report-");
        reportTickingScheduler.scheduleWithFixedDelay(() ->
                tenantResolver.getAllActiveTenants()
                    .forEach(tenant -> {
                        runtimeTenantResolver.forceTenant(tenant);
                        try {
                            self.notifyErrorsBatch(tenant);
                        } finally {
                            runtimeTenantResolver.clearTenant();
                        }
                    }),
            Duration.ofSeconds(reportTick)
        );
    }

    public void notifyErrorsBatch(String tenant) {
        Map<String, Tuple2<List<String>, Long>> tenantErrors =
            errors.getAndUpdate(m ->
                    // Imagine this operation is executed after the rest of the code if you are not used to AtomicReference and vavr Map
                m.filterKeys(k -> !k.getTenant().equals(tenant))
            )
                .filterKeys(k -> k.getTenant().equals(tenant))
                .mapKeys(QuotaKey::getUserEmail);

        self.notifyTenantErrors(tenantErrors);
    }

    public void notifyTenantErrors(Map<String, Tuple2<List<String>, Long>> userErrors) {
        userErrors.forEach((userEmail, t) -> Try.run(() -> {
            List<String> messages = t._1;
            Long errors = t._2;
            self.notifyUserErrors(userEmail, messages, errors);
        }));
    }

    public void notifyUserErrors(String userEmail, List<String> messages, Long errors) {
        Try.run(() -> {
            StringBuilder notification =
                Joiner.on("\n").appendTo(new StringBuilder(), messages);
            if (errors > 0) {
                notification.append(String.format(MORE_DOWNLOAD_ERRORS_TEMPLATE, errors));
            }
            notificationClient.notify(
                notification.toString(),
                TITLE,
                NotificationLevel.INFO,
                MimeTypeUtils.TEXT_PLAIN,
                new String[]{userEmail}
            );
        });
    }

    @Override
    public void report(DownloadLimitExceededException.DownloadQuotaExceededException e, DownloadableFile file, String email, String tenant) {
        report(
            () -> String.format(
                "Could not download file %s: download quota exceeded.",
                file.getFileName()
            ),
            email,
            tenant
        );
    }

    @Override
    public void report(DownloadLimitExceededException.DownloadRateExceededException e, DownloadableFile file, String email, String tenant) {
        report(
            () -> String.format(
                "Could not download file %s: download rate exceeded.",
                file.getFileName()
            ),
            email,
            tenant
        );
    }

    /**
     * @param message as a Supplier so that the String is not built if not needed
     * @param email
     * @param tenant
     */
    @VisibleForTesting
    protected void report(Supplier<String> message, String email, String tenant) {
        QuotaKey key = QuotaKey.make(tenant, email);
        errors.updateAndGet(m -> m
            .computeIfAbsent(key, k -> Tuple.of(List.empty(), 0L))
            ._2
            .computeIfPresent(key, (k, t) -> {
                if (t._1.size() < ELLIPSIS_THRESHOLD) {
                    return t.map1(l -> l.append(message.get()));
                }
                return t.map2(c -> c + 1);
            })
            ._2
        );
    }
}
