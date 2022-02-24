package fr.cnes.regards.modules.storage.service.file.download;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.MimeTypeUtils;
import org.testcontainers.shaded.com.google.common.base.Joiner;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static fr.cnes.regards.modules.storage.service.file.download.DownloadQuotaExceededReporter.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DownloadQuotaExceededReporterTest {

    private static final String TENANT = "default";
    public static final Random RAND = new Random();

    private long reportTick = 30;

    @Mock private ThreadPoolTaskScheduler reportTickingScheduler;

    @Mock private INotificationClient notificationClient;

    @Mock private ITenantResolver tenantResolver;

    @Mock private IRuntimeTenantResolver runtimeTenantResolver;

    @Mock private ApplicationContext applicationContext;

    @Mock private Environment env;

    private DownloadQuotaExceededReporter quotaReporter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doNothing()
            .when(runtimeTenantResolver)
            .forceTenant(anyString());
        doReturn(TENANT)
            .when(runtimeTenantResolver)
            .getTenant();

        quotaReporter = spy(
            new DownloadQuotaExceededReporter(
                reportTick,
                reportTickingScheduler,
                notificationClient,
                tenantResolver,
                runtimeTenantResolver,
                applicationContext,
                env, quotaReporter
            )
        );

        quotaReporter.setSelf(quotaReporter);
        quotaReporter.setErrors(new AtomicReference<>(HashMap.empty()));
    }

    @After
    public void tearDown() {
        Mockito.clearInvocations(runtimeTenantResolver, notificationClient);
    }

    @Test
    public void cache_is_coherent_in_the_face_of_concurrent_clients() throws InterruptedException {
        // given
        String email = UUID.randomUUID().toString();
        // cache is empty
        AtomicReference<Map<QuotaKey, Tuple2<List<String>, Long>>> cache = new AtomicReference<>(HashMap.empty());
        quotaReporter.setErrors(cache);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // when
        String err = "Oh noes!";
        int errorsProduced = RAND.nextInt(100)+1;
        CountDownLatch latch = new CountDownLatch(errorsProduced);
        for (int i = 0; i<errorsProduced; i++) {
            CompletableFuture.runAsync(
                () -> {
                    quotaReporter.report(() -> err, email, TENANT);
                    latch.countDown();
                },
                executor
            );
        }
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        // then
        Tuple2<List<String>, Long> userReports = cache.get().get(QuotaKey.make(TENANT, email)).get();
        assertTrue(userReports._1.forAll(err::equals));
        if (errorsProduced > ELLIPSIS_THRESHOLD) {
            assertEquals(ELLIPSIS_THRESHOLD, userReports._1.size());
            assertEquals(errorsProduced - ELLIPSIS_THRESHOLD, userReports._2);
        } else {
            assertEquals(errorsProduced, userReports._1.size());
            assertEquals(0L, userReports._2);
        }
    }

    @Test
    public void notifyUserErrors_should_concatenate_errors_and_send_notification() {
        // given
        String email = UUID.randomUUID().toString();
        long errorsProduced = RAND.nextInt(10)+1;
        long moreErrors = RAND.nextInt(10)+1;
        List<String> messages =
            List.ofAll(
                LongStream.range(0, errorsProduced)
                    .mapToObj(ignored -> UUID.randomUUID().toString())
                    .collect(Collectors.toList())
            );

        // when
        quotaReporter.notifyUserErrors(email, messages, moreErrors);

        // then
        ArgumentCaptor<String> messageArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationClient)
            .notify(
                messageArgumentCaptor.capture(),
                eq(TITLE),
                eq(NotificationLevel.INFO),
                eq(MimeTypeUtils.TEXT_PLAIN),
                ArgumentMatchers.<String[]>any() // fucked-up arity overloads makes mockito crazy so no `eq` here
            );
        assertEquals(
            Joiner.on("\n")
                .appendTo(new StringBuilder(), messages)
                .append(String.format(MORE_DOWNLOAD_ERRORS_TEMPLATE, moreErrors))
                .toString(),
            messageArgumentCaptor.getValue()
        );
    }

    @Test
    public void reporting_is_consistent_even_during_periodic_notifyErrorsBatch() throws InterruptedException {
        // given
        String email = UUID.randomUUID().toString();
        // cache is empty
        AtomicReference<Map<QuotaKey, Tuple2<List<String>, Long>>> cache = new AtomicReference<>(HashMap.empty());
        quotaReporter.setErrors(cache);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // a bunch of clients
        int clients = 5;
        // report a bunch of errors
        // (the cache coherence of one or many clients reporting errors concurrently is ensured by another test)
        String err = "Oh noes!";
        int errorsProducedOnFirstRound = RAND.nextInt(1000)+1;
        CountDownLatch latch = new CountDownLatch(errorsProducedOnFirstRound);
        for (int i = 0; i<errorsProducedOnFirstRound; i++) {
            CompletableFuture.runAsync(
                () -> {
                    quotaReporter.report(() -> err, makeInLoopEmail(email, RAND.nextInt(clients)), TENANT);
                    latch.countDown();
                },
                executor
            );
        }
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();
        // errors reported, cache should be filled

        // now the batch of errors messages is sent (simulating scheduler tick)
        // but the real method is tweaked so that we ensure than
        // a bunch of clients report a few more errors concurrently

        // for some reason I can't seem to find a way to mock this bloody notificationClient
        // so I'm hacking the notifyUserErrors method instead...
        doAnswer(a -> {
            String user = a.getArgument(0);
            quotaReporter.report(() -> err, user, TENANT);
            return a.callRealMethod();
        }).when(quotaReporter)
            .notifyUserErrors(anyString(), any(), anyLong());

        quotaReporter.notifyErrorsBatch(TENANT);

        // then
        // the cache should contain all the errors reported during notification batch
        IntStream.range(0, clients)
            .forEach(clientIdx -> {
                QuotaKey key = QuotaKey.make(TENANT, makeInLoopEmail(email, clientIdx));
                if (cache.get().containsKey(key)) {
                    Tuple2<List<String>, Long> userReports = cache.get().get(key).get();
                    assertEquals(1, userReports._1.size());
                    assertEquals(0L, userReports._2);
                } else {
                    Assert.fail(String.format("Missing key in errors report for %s/%s-%s. cache size=%s",TENANT, email, clientIdx, cache.get().size()));
                }
            });
    }

    @NotNull
    protected String makeInLoopEmail(String email, int idx) {
        return email + "-" + idx;
    }
}
