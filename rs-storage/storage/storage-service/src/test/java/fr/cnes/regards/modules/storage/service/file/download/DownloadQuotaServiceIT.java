package fr.cnes.regards.modules.storage.service.file.download;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.storage.domain.database.DefaultDownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserQuotaAggregate;
import fr.cnes.regards.modules.storage.domain.database.UserRateAggregate;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.control.Try;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestPropertySource( properties = { "spring.jpa.properties.hibernate.default_schema=storage_download_quota_tests"  })
@RunWith(SpringRunner.class)
public class DownloadQuotaServiceIT extends AbstractRegardsTransactionalIT {

    public static final long MAX_QUOTA = 10L;
    public static final long RATE_LIMIT = 600L;
    @Autowired private IDownloadQuotaRepository quotaRepository;

    private IDownloadQuotaRepository quotaRepositoryDelegate;

    @MockBean
    private IQuotaManager quotaManager;

    @Autowired private IRuntimeTenantResolver tenantResolver;

    @Mock private ISubscriber subscriber;

    @Autowired @InjectMocks private DownloadQuotaService<Unit> quotaService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        quotaRepositoryDelegate = quotaRepository;
        quotaRepository =
            Mockito.mock(IDownloadQuotaRepository.class, AdditionalAnswers.delegatesTo(quotaRepository));
        ReflectionTestUtils.setField(quotaService, "quotaRepository", quotaRepository);

        tenantResolver.forceTenant(getDefaultTenant());
        quotaService.setCache(Caffeine.newBuilder().build());
        quotaService.setDefaultLimits(new AtomicReference<>(HashMap.of(getDefaultTenant(), new DefaultDownloadQuotaLimits(MAX_QUOTA, RATE_LIMIT))));
    }

    @After
    public void tearDown() {
        quotaRepository.deleteAll();
        Mockito.clearInvocations(quotaRepository, quotaManager);
    }

    /**
     * Test that when no entry exists in cache for a particular user and
     * no quota record exists in the database for this users, then
     * the record is inserted but if this write fails because of
     * another write is made concurrently for this user then the operation
     * shouldn't fail, a new read should simply happen.
     */
    @Test
    public void should_retry_find_request_for_caching_when_concurrent_quota_insert_happens() {
        // given
        String userEmail = "foo@bar.com";
        DownloadQuotaLimits stub = new DownloadQuotaLimits(getDefaultTenant(), userEmail, MAX_QUOTA, RATE_LIMIT);

        // pretend there's no entry for the target user in the cache
        QuotaKey key = QuotaKey.make(getDefaultTenant(), userEmail);
        Cache<QuotaKey, DownloadQuotaLimits> cache = Caffeine.newBuilder().build();
        quotaService.setCache(cache);

        // and
        // a concurrent write happens while inserting the new quota record
        doAnswer(a -> {
            DownloadQuotaLimits newDownloadQuota = a.getArgument(0, DownloadQuotaLimits.class);
            Thread concurrentOperation =
                new Thread(() -> {
                    tenantResolver.forceTenant(getDefaultTenant());
                    quotaRepositoryDelegate.save(new DownloadQuotaLimits(newDownloadQuota));
                });
            concurrentOperation.start();
            concurrentOperation.join();
            return quotaRepositoryDelegate.save(newDownloadQuota);
        }).when(quotaRepository).save(any());

//        // but pretend it doesn't exist at first read
//        // so the existing record appears to the service as a concurrent write
//        // when it tries to create it
//        doReturn(Optional.empty())
//            .when(quotaRepository)
//            .findByUserEmail(userEmail);

        // also, the quotaManager is bypassed completely because we don't care in this test
        doReturn(
            Tuple.of(
                new UserQuotaAggregate(0L),
                new UserRateAggregate(0L)
            )
        ).when(quotaManager).get(any());
        doNothing().when(quotaManager).increment(any());
        doNothing().when(quotaManager).decrement(any());

        // when
        // we execute a noop
        Try<Unit> result = quotaService.withQuota(userEmail, quotaHandler ->
            Try.success(Unit.UNIT)
                .peek(__ -> quotaHandler.start())
                .peek(__ -> quotaHandler.stop())
        );

        // then
        // a first find request is made to the quotaRepository but returns nothing
        // and a second is made to retry after the concurrent insertion fails (see below)
        verify(quotaRepository, times(2)).findByEmail(userEmail);

        // a save request is made (but fails) to insert the default quota,
        ArgumentCaptor<DownloadQuotaLimits> argument = ArgumentCaptor.forClass(DownloadQuotaLimits.class);
        verify(quotaRepository).save(argument.capture());
        assertEquals(userEmail, argument.getValue().getEmail());
        assertEquals(MAX_QUOTA, argument.getValue().getMaxQuota().longValue());

        // no more requests are made to the quotaRepository,
        verifyNoMoreInteractions(quotaRepository);

        // the quota value is cached,
        assertEquals(MAX_QUOTA, cache.getIfPresent(key).getMaxQuota().longValue());

        // and
        // we finally got the noop result
        result.get();
        assertTrue(result.isSuccess());
    }
}

