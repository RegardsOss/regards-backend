/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.storage.rest;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.fileaccess.dto.quota.DownloadQuotaLimitsDto;
import fr.cnes.regards.modules.storage.dao.entity.mapping.DomainEntityMapper;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import fr.cnes.regards.modules.storage.service.file.download.DownloadQuotaService;
import fr.cnes.regards.modules.storage.service.file.download.QuotaKey;
import io.vavr.collection.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMethod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.cnes.regards.modules.storage.rest.DownloadQuotaController.*;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=quota_rest_it" })
@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
public class DownloadQuotaControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IDownloadQuotaRepository quotaRepository;

    @SuppressWarnings("rawtypes")
    @Autowired
    private DownloadQuotaService quotaService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private DomainEntityMapper mapper;

    private final Random random = new Random();

    private Cache<QuotaKey, DownloadQuotaLimits> userLimitsCache;

    @Before
    @After
    public void clean() {
        tenantResolver.forceTenant(getDefaultTenant());
        quotaRepository.deleteAll();
        userLimitsCache = Caffeine.newBuilder().build();
        quotaService.setCache(userLimitsCache);
    }

    @Test
    public void getQuotaLimits_should_create_limits_for_parameterized_user_with_default_value_if_not_exist() {
        String userEmail = UUID.randomUUID().toString();

        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expectValue("$.maxQuota", -1L)
                                                          .expectValue("$.rateLimit", -1L);

        performDefaultGet(PATH_USER_QUOTA, customizer, "Failed to get user quota limits", userEmail);

        DownloadQuotaLimits cached = userLimitsCache.getIfPresent(QuotaKey.make(getDefaultTenant(), userEmail));
        assertEquals(-1L, cached.getMaxQuota().longValue());
        assertEquals(-1L, cached.getRateLimit().longValue());
    }

    @Test
    public void getQuotaLimits_should_return_limits_for_parameterized_user_if_exist() {
        String userEmail = UUID.randomUUID().toString();
        long maxQuota = random.nextInt(Integer.MAX_VALUE);
        long rateLimit = random.nextInt(Integer.MAX_VALUE);
        quotaRepository.save(new DownloadQuotaLimits(getDefaultTenant(), userEmail, maxQuota, rateLimit));

        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expectValue("$.maxQuota", maxQuota)
                                                          .expectValue("$.rateLimit", rateLimit);

        performDefaultGet(PATH_USER_QUOTA, customizer, "Failed to get user quota limits", userEmail);

        DownloadQuotaLimits cached = userLimitsCache.getIfPresent(QuotaKey.make(getDefaultTenant(), userEmail));
        assertEquals(maxQuota, cached.getMaxQuota().longValue());
        assertEquals(rateLimit, cached.getRateLimit().longValue());
    }

    @Test
    public void upsertQuotaLimits_should_create_limits_for_user_if_not_exist() {
        String userEmail = UUID.randomUUID().toString();

        long maxQuota = random.nextInt(Integer.MAX_VALUE);
        long rateLimit = random.nextInt(Integer.MAX_VALUE);
        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expectValue("$.maxQuota", maxQuota)
                                                          .expectValue("$.rateLimit", rateLimit);

        DownloadQuotaLimitsDto dto = new DownloadQuotaLimitsDto(userEmail, maxQuota, rateLimit);
        performDefaultPut(PATH_USER_QUOTA, dto, customizer, "Failed to create user quota limits", userEmail);

        customizer = customizer().expectStatusOk()
                                 .expectValue("$.maxQuota", maxQuota)
                                 .expectValue("$.rateLimit", rateLimit);
        performDefaultGet(PATH_USER_QUOTA, customizer, "Failed to get new user quota limits", userEmail);

        DownloadQuotaLimits cached = userLimitsCache.getIfPresent(QuotaKey.make(getDefaultTenant(), userEmail));
        assertEquals(maxQuota, cached.getMaxQuota().longValue());
        assertEquals(rateLimit, cached.getRateLimit().longValue());
    }

    @Test
    public void upsertQuotaLimits_should_update_limits_for_user_if_exist() {
        String userEmail = UUID.randomUUID().toString();

        // init
        long maxQuota = random.nextInt(Integer.MAX_VALUE);
        long rateLimit = random.nextInt(Integer.MAX_VALUE);
        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expectValue("$.maxQuota", maxQuota)
                                                          .expectValue("$.rateLimit", rateLimit);

        DownloadQuotaLimitsDto dto = new DownloadQuotaLimitsDto(userEmail, maxQuota, rateLimit);
        performDefaultPut(PATH_USER_QUOTA, dto, customizer, "Failed to create user quota limits", userEmail);

        customizer = customizer().expectStatusOk()
                                 .expectValue("$.maxQuota", maxQuota)
                                 .expectValue("$.rateLimit", rateLimit);
        performDefaultGet(PATH_USER_QUOTA, customizer, "Failed to get new user quota limits", userEmail);

        // update
        maxQuota = random.nextInt(Integer.MAX_VALUE);
        rateLimit = random.nextInt(Integer.MAX_VALUE);
        customizer = customizer().expectStatusOk()
                                 .expectValue("$.maxQuota", maxQuota)
                                 .expectValue("$.rateLimit", rateLimit);

        dto = new DownloadQuotaLimitsDto(userEmail, maxQuota, rateLimit);
        performDefaultPut(PATH_USER_QUOTA, dto, customizer, "Failed to update user quota limits", userEmail);

        customizer = customizer().expectStatusOk()
                                 .expectValue("$.maxQuota", maxQuota)
                                 .expectValue("$.rateLimit", rateLimit);
        performDefaultGet(PATH_USER_QUOTA, customizer, "Failed to get updated user quota limits", userEmail);

        DownloadQuotaLimits cached = userLimitsCache.getIfPresent(QuotaKey.make(getDefaultTenant(), userEmail));
        assertEquals(maxQuota, cached.getMaxQuota().longValue());
        assertEquals(rateLimit, cached.getRateLimit().longValue());
    }

    @Test
    public void getQuotaLimits_should_create_limits_for_current_user_with_default_value_if_not_exist() {
        String userEmail = UUID.randomUUID().toString();

        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expectValue("$.maxQuota", -1L)
                                                          .expectValue("$.rateLimit", -1L);

        String token = manageSecurity(getDefaultTenant(), PATH_QUOTA, RequestMethod.GET, userEmail, getDefaultRole());
        performGet(PATH_QUOTA, token, customizer, "Failed to get user quota limits");

        DownloadQuotaLimits cached = userLimitsCache.getIfPresent(QuotaKey.make(getDefaultTenant(), userEmail));
        assertEquals(-1L, cached.getMaxQuota().longValue());
        assertEquals(-1L, cached.getRateLimit().longValue());
    }

    @Test
    public void getQuotaLimits_should_return_limits_for_current_user_if_exist() {
        String userEmail = UUID.randomUUID().toString();
        long maxQuota = random.nextInt(Integer.MAX_VALUE);
        long rateLimit = random.nextInt(Integer.MAX_VALUE);
        quotaRepository.save(new DownloadQuotaLimits(getDefaultTenant(), userEmail, maxQuota, rateLimit));

        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expectValue("$.maxQuota", maxQuota)
                                                          .expectValue("$.rateLimit", rateLimit);

        String token = manageSecurity(getDefaultTenant(), PATH_QUOTA, RequestMethod.GET, userEmail, getDefaultRole());
        performGet(PATH_QUOTA, token, customizer, "Failed to get user quota limits");

        DownloadQuotaLimits cached = userLimitsCache.getIfPresent(QuotaKey.make(getDefaultTenant(), userEmail));
        assertEquals(maxQuota, cached.getMaxQuota().longValue());
        assertEquals(rateLimit, cached.getRateLimit().longValue());
    }

    @Test
    public void getQuotaLimits_should_return_a_list_of_user_quota_limits() {
        List<DownloadQuotaLimits> quotaLimits = IntStream.range(0, 50)
                                                         .mapToObj(ignored -> UUID.randomUUID().toString())
                                                         .map(userEmail -> {
                                                             long maxQuota = random.nextInt(Integer.MAX_VALUE);
                                                             long rateLimit = random.nextInt(Integer.MAX_VALUE);
                                                             return quotaRepository.save(new DownloadQuotaLimits(
                                                                 getDefaultTenant(),
                                                                 userEmail,
                                                                 maxQuota,
                                                                 rateLimit));
                                                         })
                                                         .collect(Collectors.toList());

        RequestBuilderCustomizer customizer = customizer().addParameter(USER_EMAIL_PARAM,
                                                                        quotaLimits.stream()
                                                                                   .map(DownloadQuotaLimits::getEmail)
                                                                                   .toArray(String[]::new))
                                                          .expectStatusOk();

        Stream.ofAll(quotaLimits).zipWithIndex().forEach(t -> {
            customizer.expectValue(String.format("$.[%s].email", t._2), t._1.getEmail());
            customizer.expectValue(String.format("$.[%s].maxQuota", t._2), t._1.getMaxQuota());
            customizer.expectValue(String.format("$.[%s].rateLimit", t._2), t._1.getRateLimit());
        });

        performDefaultGet(PATH_QUOTA_LIST, customizer, "Failed to get user quota limits");

        //        DownloadQuotaLimits cached = userLimitsCache.getIfPresent(QuotaKey.make(getDefaultTenant(), userEmail));
        //        assertEquals(maxQuota, cached.getMaxQuota().longValue());
        //        assertEquals(rateLimit, cached.getRateLimit().longValue());
    }

    @Test
    public void getCurrentQuotas_should_return_empty_quotas_and_create_default_limits_if_not_exist() {
        String userEmail = UUID.randomUUID().toString();

        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expectValue("$.maxQuota", -1L)
                                                          .expectValue("$.rateLimit", -1L)
                                                          .expectValue("$.currentQuota", 0L)
                                                          .expectValue("$.currentRate", 0L);

        String token = manageSecurity(getDefaultTenant(),
                                      PATH_CURRENT_QUOTA,
                                      RequestMethod.GET,
                                      userEmail,
                                      getDefaultRole());
        performGet(PATH_CURRENT_QUOTA, token, customizer, "Failed to get current user quotas");

        DownloadQuotaLimits cached = userLimitsCache.getIfPresent(QuotaKey.make(getDefaultTenant(), userEmail));
        assertEquals(-1L, cached.getMaxQuota().longValue());
        assertEquals(-1L, cached.getRateLimit().longValue());
    }

    @Test
    public void getCurrentQuotas_should_return_empty_quotas_and_download_limits_for_current_user_if_exist() {
        String userEmail = UUID.randomUUID().toString();
        long maxQuota = random.nextInt(Integer.MAX_VALUE);
        long rateLimit = random.nextInt(Integer.MAX_VALUE);
        quotaRepository.save(new DownloadQuotaLimits(getDefaultTenant(), userEmail, maxQuota, rateLimit));

        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expectValue("$.maxQuota", maxQuota)
                                                          .expectValue("$.rateLimit", rateLimit)
                                                          .expectValue("$.currentQuota", 0L)
                                                          .expectValue("$.currentRate", 0L);

        String token = manageSecurity(getDefaultTenant(),
                                      PATH_CURRENT_QUOTA,
                                      RequestMethod.GET,
                                      userEmail,
                                      getDefaultRole());
        performGet(PATH_CURRENT_QUOTA, token, customizer, "Failed to get current user quotas");

        DownloadQuotaLimits cached = userLimitsCache.getIfPresent(QuotaKey.make(getDefaultTenant(), userEmail));
        assertEquals(maxQuota, cached.getMaxQuota().longValue());
        assertEquals(rateLimit, cached.getRateLimit().longValue());
    }

    @Test
    public void getCurrentQuotas_should_return_current_quotas() {
        String userEmail = UUID.randomUUID().toString();
        long maxQuota = random.nextInt(Integer.MAX_VALUE);
        long rateLimit = random.nextInt(Integer.MAX_VALUE);
        quotaRepository.save(new DownloadQuotaLimits(getDefaultTenant(), userEmail, maxQuota, rateLimit));
        String instance1 = UUID.randomUUID().toString();
        String instance2 = UUID.randomUUID().toString();
        long currentQuotaOnInstance1 = random.nextInt(Integer.MAX_VALUE);
        long currentQuotaOnInstance2 = random.nextInt(Integer.MAX_VALUE);
        long currentRateOnInstance1 = random.nextInt(Integer.MAX_VALUE);
        long currentRateOnInstance2 = random.nextInt(Integer.MAX_VALUE);
        quotaRepository.upsertOrCombineDownloadQuota(instance1, userEmail, currentQuotaOnInstance1);
        quotaRepository.upsertOrCombineDownloadQuota(instance2, userEmail, currentQuotaOnInstance2);
        quotaRepository.upsertOrCombineDownloadRate(instance1,
                                                    userEmail,
                                                    currentRateOnInstance1,
                                                    LocalDateTime.now().plusSeconds(120));
        quotaRepository.upsertOrCombineDownloadRate(instance2,
                                                    userEmail,
                                                    currentRateOnInstance2,
                                                    LocalDateTime.now().plusSeconds(120));

        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                                                          .expectValue("$.maxQuota", maxQuota)
                                                          .expectValue("$.rateLimit", rateLimit)
                                                          .expectValue("$.currentQuota",
                                                                       currentQuotaOnInstance1
                                                                       + currentQuotaOnInstance2)
                                                          .expectValue("$.currentRate",
                                                                       currentRateOnInstance1 + currentRateOnInstance2);

        String token = manageSecurity(getDefaultTenant(),
                                      PATH_CURRENT_QUOTA,
                                      RequestMethod.GET,
                                      userEmail,
                                      getDefaultRole());
        performGet(PATH_CURRENT_QUOTA, token, customizer, "Failed to get current user quotas");

        DownloadQuotaLimits cached = userLimitsCache.getIfPresent(QuotaKey.make(getDefaultTenant(), userEmail));
        assertEquals(maxQuota, cached.getMaxQuota().longValue());
        assertEquals(rateLimit, cached.getRateLimit().longValue());
    }
}
