package fr.cnes.regards.modules.storage.service.file.download;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserAction;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserEvent;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserQuotaAggregate;
import fr.cnes.regards.modules.storage.domain.database.UserRateAggregate;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import fr.cnes.regards.modules.storage.service.file.exception.DownloadQuotaLimitExceededException;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.assertj.core.api.ThrowableAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static fr.cnes.regards.modules.storage.dao.entity.download.DownloadQuotaLimitsEntity.UK_DOWNLOAD_QUOTA_LIMITS_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DownloadQuotaServiceImplTest {

    private static final String TENANT = "default";
    private static final long DEFAULT_QUOTA = 20;
    private static final long DEFAULT_RATE = 20;

    @Mock private IDownloadQuotaRepository quotaRepository;

    @Mock private IQuotaManager quotaManager;

    @Mock private IRuntimeTenantResolver tenantResolver;

    @Mock private ISubscriber subscriber;

    @Mock private ApplicationContext applicationContext;

    private DownloadQuotaServiceImpl<Unit> quotaService;

    private final Random random = new Random();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doNothing()
            .when(tenantResolver)
            .forceTenant(anyString());
        doReturn(TENANT)
            .when(tenantResolver)
            .getTenant();

        quotaService = spy(
            new DownloadQuotaServiceImpl<>(
                DEFAULT_QUOTA,
                DEFAULT_RATE,
                quotaRepository,
                quotaManager,
                tenantResolver,
                subscriber,
                applicationContext
            )
        );
        quotaService.setSelf(quotaService);
        quotaService.setCache(Caffeine.newBuilder().build());
    }

    @After
    public void tearDown() {
        quotaRepository.deleteAll();
        Mockito.clearInvocations(quotaRepository, quotaManager);
    }

    @Test
    public void createDefaultDownloadQuota_should_insert_default_quota() {
        // given
        String userEmail = "foo@bar.com";

        // when
        DownloadQuotaLimits stub = new DownloadQuotaLimits(TENANT, userEmail, DEFAULT_QUOTA, DEFAULT_RATE);
        doReturn(stub)
            .when(quotaRepository)
            .save(any());
        DownloadQuotaLimits downloadQuota =
            quotaService.createDefaultDownloadQuota(userEmail);

        // then
        assertEquals(stub, downloadQuota);
    }

    @Test
    public void createDefaultDownloadQuota_should_rethrow_exceptions() {
        // given
        String userEmail = "foo@bar.com";
        Exception expected = new RuntimeException("expected");

        // when
        doThrow(expected)
            .when(quotaRepository)
            .save(any());
        ThrowableAssert.ThrowingCallable throwing =
            () -> quotaService.createDefaultDownloadQuota(userEmail);

        // then
        assertThatThrownBy(throwing)
            .isEqualTo(expected);
    }

    @Test
    public void findOrCreateDownloadQuota_should_get_quota_if_exists() {
        // given
        String userEmail = "foo@bar.com";
        DownloadQuotaLimits stub = new DownloadQuotaLimits(TENANT, userEmail, DEFAULT_QUOTA, DEFAULT_RATE);

        // there exist a quota for the target user
        doReturn(Optional.of(stub))
            .when(quotaRepository)
            .findByEmail(userEmail);

        // when
        // we search for it
        DownloadQuotaLimits downloadQuota =
            quotaService.findOrCreateDownloadQuota(userEmail);

        // then
        // we find it
        verify(quotaRepository).findByEmail(userEmail);
        assertEquals(stub, downloadQuota);

        // and no other db operation
        verifyNoMoreInteractions(quotaRepository);
    }

    @Test
    public void findOrCreateDownloadQuota_should_insert_quota_if_not_exists() {
        // given
        String userEmail = "foo@bar.com";
        // there is no quota for the target user
        doReturn(Optional.empty())
            .when(quotaRepository)
            .findByEmail(userEmail);

        // when
        // we search for it
        DownloadQuotaLimits stub = new DownloadQuotaLimits(TENANT, userEmail, DEFAULT_QUOTA, DEFAULT_RATE);
        doReturn(stub)
            .when(quotaRepository)
            .save(any());
        DownloadQuotaLimits downloadQuota =
            quotaService.findOrCreateDownloadQuota(userEmail);

        // then
        // we don't find it
        verify(quotaRepository).findByEmail(userEmail);

        // so we create it
        verify(quotaRepository).save(any());
        assertEquals(stub, downloadQuota);

        // and no other db operation
        verifyNoMoreInteractions(quotaRepository);
    }

    @Test
    public void findOrCreateDownloadQuota_should_retry_search_on_concurrent_quota_insert_error() {
        // given
        String userEmail = "foo@bar.com";
        DownloadQuotaLimits stub = new DownloadQuotaLimits(TENANT, userEmail, DEFAULT_QUOTA, DEFAULT_RATE);
        // there is no quota for the target user at first
        doReturn(Optional.empty())
            .doReturn(Optional.of(stub))
            .when(quotaRepository)
            .findByEmail(userEmail);

        // when
        // we search for it
        doThrow(
            new DataIntegrityViolationException(
                "expected",
                new PSQLException(UK_DOWNLOAD_QUOTA_LIMITS_EMAIL, PSQLState.UNKNOWN_STATE)
            ))
            .when(quotaRepository)
            .save(any());
        DownloadQuotaLimits downloadQuota =
            quotaService.findOrCreateDownloadQuota(userEmail);

        // then
        // we didn't find it at first but we retried
        verify(quotaRepository, times(2)).findByEmail(userEmail);
        assertEquals(stub, downloadQuota);

        // and we tried to create
        verify(quotaRepository).save(any());

        // and no other db operation
        verifyNoMoreInteractions(quotaRepository);
    }

    @Test
    public void findOrCreateDownloadQuota_should_rethrow_on_other_errors() {
        // given
        String userEmail = "foo@bar.com";
        // there is no quota for the target user at first
        doReturn(Optional.empty())
            .when(quotaRepository)
            .findByEmail(userEmail);

        {
            // when
            // we search for it
            DataIntegrityViolationException expected =
                new DataIntegrityViolationException(
                    "expected",
                    new PSQLException("anything but UK error", PSQLState.UNKNOWN_STATE)
                );
            doThrow(expected)
                .when(quotaRepository)
                .save(any());
            ThrowableAssert.ThrowingCallable throwing =
                () -> quotaService.findOrCreateDownloadQuota(userEmail);

            // then
            // we didn't find it at first so we tried to create it but failed
            assertThatThrownBy(throwing)
                .isEqualTo(expected);
            verify(quotaRepository).findByEmail(userEmail);
            verify(quotaRepository).save(any());

            // and no other db operation
            verifyNoMoreInteractions(quotaRepository);
        }

        {
            Mockito.clearInvocations(quotaRepository);
            // when
            // we search for it
            Exception expected = new RuntimeException("expected");
            doThrow(expected)
                .when(quotaRepository)
                .save(any());
            ThrowableAssert.ThrowingCallable throwing =
                () -> quotaService.findOrCreateDownloadQuota(userEmail);

            // then
            // we didn't find it at first so we tried to create it but failed
            assertThatThrownBy(throwing)
                .isEqualTo(expected);
            verify(quotaRepository).findByEmail(userEmail);
            verify(quotaRepository).save(any());

            // and no other db operation
            verifyNoMoreInteractions(quotaRepository);
        }
    }

    @Test
    public void cacheUserQuota_should_cache_quota_if_not_cached() {
        // given
        String userEmail = "foo@bar.com";

        // there's no entry for the target user in the cache (nor DB)
        QuotaKey key = QuotaKey.make(TENANT, userEmail);
        Cache<QuotaKey, DownloadQuotaLimits> cache = Caffeine.newBuilder().build();
        quotaService.setCache(cache);

        // when
        // we pretend to insert it in DB
        long quotaStub = random.nextInt(Integer.MAX_VALUE);
        long rateStub = random.nextInt(Integer.MAX_VALUE);
        doReturn(new DownloadQuotaLimits(TENANT, userEmail, quotaStub, rateStub))
            .when(quotaService)
            .findOrCreateDownloadQuota(userEmail);
        Try<DownloadQuotaLimits> result = quotaService.cacheUserQuota(userEmail, key);

        // then
        assertTrue(result.isSuccess());
        assertEquals(quotaStub, result.get().getMaxQuota().longValue());
        assertEquals(quotaStub, cache.getIfPresent(key).getMaxQuota().longValue());
        assertEquals(rateStub, result.get().getRateLimit().longValue());
        assertEquals(rateStub, cache.getIfPresent(key).getRateLimit().longValue());
    }

    @Test
    public void getUserQuotaAndRate_should_return_quota_and_rate_if_not_exceeded() {
        // given
        String userEmail = "foo@bar.com";
        QuotaKey key = QuotaKey.make(TENANT, userEmail);
        long maxQuota = random.nextInt(Integer.MAX_VALUE);
        long rateLimit = random.nextInt(Integer.MAX_VALUE);
        DownloadQuotaLimits quota = new DownloadQuotaLimits(TENANT, userEmail, maxQuota, rateLimit);

        IntStream.range(0, 1_000)
            .forEach(ignored -> {
                long stubQuotaCounter = random.nextInt(Integer.MAX_VALUE);
                long stubRateGauge = random.nextInt(Integer.MAX_VALUE);
                doReturn(
                    CompletableFuture.completedFuture(
                        Tuple.of(
                            new UserQuotaAggregate(stubQuotaCounter),
                            new UserRateAggregate(stubRateGauge)
                        )
                    )
                ).when(quotaManager).get(quota);

                // when we try get it
                Try<Tuple3<DownloadQuotaLimits, Long, Long>> result = quotaService.getUserQuotaAndRate(quota);

                // then
                if (stubQuotaCounter < maxQuota && stubRateGauge < rateLimit) {
                    assertTrue(result.isSuccess());
                    assertEquals(quota, result.get()._1);
                    assertEquals(stubQuotaCounter, result.get()._2.longValue());
                    assertEquals(stubRateGauge, result.get()._3.longValue());
                } else {
                    assertTrue(result.isFailure());
                    assertThat(result.getCause())
                        .isInstanceOf(DownloadQuotaLimitExceededException.class);
                }
            });
    }

    @Test
    public void apply_should_execute_operation_and_manage_quota_if_caller_respects_quotaHandler_contract() {
        // given
        String userEmail = "foo@bar.com";
        QuotaKey key = QuotaKey.make(TENANT, userEmail);
        long stub = random.nextInt(Integer.MAX_VALUE);
        DownloadQuotaLimits quota = new DownloadQuotaLimits(TENANT, userEmail, stub, DEFAULT_RATE);
        AtomicLong stubGauge = new AtomicLong(stub);
        doAnswer(a -> CompletableFuture.completedFuture(stubGauge.incrementAndGet()))
            .when(quotaManager)
            .increment(quota);
        doAnswer(a -> CompletableFuture.completedFuture(stubGauge.decrementAndGet()))
            .when(quotaManager)
            .decrement(quota);

        // when
        Try<Unit> result = quotaService.apply(quotaHandler ->
                Try.success(Unit.UNIT)
                    .peek(__ -> quotaHandler.start())
                    .peek(__ -> quotaHandler.stop()),
            quota
        );

        // then
        // we get the operation's result
        assertTrue(result.isSuccess());
        assertEquals(Unit.UNIT, result.get());

        // and the gauge has been both incremented and then decremented
        assertEquals(stub, stubGauge.get());
        verify(quotaManager).increment(quota);
        verify(quotaManager).decrement(quota);
    }

    @Test
    public void apply_cannot_do_magic_however() {
        // given
        String userEmail = "foo@bar.com";
        QuotaKey key = QuotaKey.make(TENANT, userEmail);

        {
            DownloadQuotaLimits quota = new DownloadQuotaLimits(TENANT, userEmail, DEFAULT_QUOTA, DEFAULT_RATE);

            // when
            // the caller doesn't respect the contract and forgets to call start/stop
            Try<Unit> result = quotaService.apply(
                quotaHandler ->
                    Try.success(Unit.UNIT),
                quota
            );

            // then
            // we get the operation's result
            assertTrue(result.isSuccess());
            assertEquals(Unit.UNIT, result.get());

            // and the quota manager is left untouched
            verifyNoInteractions(quotaManager);
        }
        {
            DownloadQuotaLimits quota = new DownloadQuotaLimits(TENANT, userEmail, DEFAULT_QUOTA, DEFAULT_RATE);

            // when
            // the caller doesn't handle failure at all
            Exception expected = new Exception("expected");
            Try<Unit> result = quotaService.apply(
                quotaHandler ->
                    Try.<Unit>failure(expected)
                        .peek(__ -> quotaHandler.start())
                        .peek(__ -> quotaHandler.stop()),
                quota
            );

            // then
            // we get the operation's result
            assertTrue(result.isFailure());
            assertEquals(expected, result.getCause());

            // and the quota manager is left untouched
            verifyNoInteractions(quotaManager);
        }
        {
            long stub = random.nextInt(Integer.MAX_VALUE);
            DownloadQuotaLimits quota = new DownloadQuotaLimits(TENANT, userEmail, stub, DEFAULT_RATE);
            AtomicLong stubGauge = new AtomicLong(stub);
            doAnswer(a -> CompletableFuture.completedFuture(stubGauge.incrementAndGet()))
                .when(quotaManager)
                .increment(quota);

            // when
            // the caller call start but then doesn't handle failure properly
            Exception expected = new Exception("expected");
            Try<Unit> result = quotaService.apply(
                quotaHandler ->
                    Try.success(Unit.UNIT)
                        .peek(__ -> quotaHandler.start())
                        .<Unit>mapTry(__ -> { throw expected; })
                        .peek(__ -> quotaHandler.stop()),
                quota
            );

            // then
            // we get the operation's result
            assertTrue(result.isFailure());
            assertEquals(expected, result.getCause());

            // and the gauge has been incremented but not decremented
            assertEquals(1, stubGauge.get() - stub);
            verify(quotaManager).increment(quota);
            verifyNoMoreInteractions(quotaManager);
        }
    }

    @Test
    public void handleBatch_should_remove_user_quota() {
        // given
        String userEmail = "foo@bar.com";

        // there's an entry for the target user in the cache
        QuotaKey key = QuotaKey.make(TENANT, userEmail);
        DownloadQuotaLimits quota = new DownloadQuotaLimits(TENANT, userEmail, DEFAULT_QUOTA, DEFAULT_RATE);
        Cache<QuotaKey, DownloadQuotaLimits> cache = Caffeine.newBuilder().build();
        cache.put(key, quota);
        quotaService.setCache(cache);

        doNothing()
            .when(quotaRepository)
            .deleteByEmail(userEmail);

        // when
        quotaService.handleBatch(
            TENANT,
            Lists.newArrayList(
                new ProjectUserEvent(userEmail, ProjectUserAction.DELETION))
        );

        // then
        assertTrue(Option.of(cache.getIfPresent(key)).isEmpty());
        verify(quotaRepository).deleteByEmail(userEmail);
    }
}

