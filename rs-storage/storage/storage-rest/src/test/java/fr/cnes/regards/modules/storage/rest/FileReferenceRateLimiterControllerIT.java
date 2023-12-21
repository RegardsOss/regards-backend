/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.jayway.jsonpath.JsonPath;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import io.vavr.collection.List;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.RequestMethod;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * This class aims to test the "GetQuota" endpoint while saturating the service with downloads request.
 * The service will get some trouble on download endpoint while the GetQuota endpoint
 * works well.
 * The min pool size and max pool size is set to 2 to get easy DB pool saturation
 *
 * @author Sébastien Binda
 * @author Léo Mieulet
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_rest_it",
                                   "regards.storage.quota.report.tick=1",
                                   "regards.jpa.multitenant.minPoolSize=2",
                                   "regards.jpa.multitenant.maxPoolSize=2",
                                   "regards.amqp.enabled=true" })
@ActiveProfiles(value = { "testAmqp", "default", "test" }, inheritProfiles = false)
public class FileReferenceRateLimiterControllerIT extends AbstractFileReferenceControllerIT {

    @Ignore("this test requires a lot of CPU to be able to succeed, thing we dont have on our CI")
    @Test
    public void rate_limiting_ends_eventually() throws InterruptedException {

        tenantResolver.forceTenant(getDefaultTenant());

        String userEmail = UUID.randomUUID().toString();
        long maxQuota = -1L; // unlimited because IDK how many retries will be needed until the rate limiter gets angry
        long rateLimit = 2L; // low enough in order to increase the chance of hitting the rate limiter
        quotaRepository.save(new DownloadQuotaLimits(getDefaultTenant(), userEmail, maxQuota, rateLimit));

        AtomicReference<List<Integer>> downloadReqStatuses = new AtomicReference<>(List.empty());
        AtomicReference<List<Integer>> currentRatesHistory = new AtomicReference<>(List.empty());

        // try to exceed the rate limit
        int nbDownloads = 100;
        // avoid using more than 3 threads, or less if you have few CPU
        int maxConcurrency = Math.min(Runtime.getRuntime().availableProcessors(), 3);

        // latch to know when all the downloads are finished (independently of the nb of retries)
        CountDownLatch latch = new CountDownLatch(nbDownloads);

        // periodically check current rate and store the observed value
        JsonPath jsonPath = JsonPath.compile("$.currentRate");
        Disposable monitor = Flux.interval(Duration.ofMillis(50L)).concatMap(i -> Mono.defer(() -> {
                                     RequestBuilderCustomizer requestBuilderCustomizer = customizer()
                                         // let assertion pass, I want to accumulate status codes!
                                         .expect(r -> assertTrue(true));
                                     String api = DownloadQuotaController.PATH_CURRENT_QUOTA;
                                     String authToken = manageSecurity(getDefaultTenant(), api, RequestMethod.GET, userEmail, getDefaultRole());
                                     ResultActions res = performGet(api,
                                                                    authToken,
                                                                    requestBuilderCustomizer,
                                                                    "Get current quotas should not blow up");
                                     MvcResult result = res.andReturn();

                                     try {
                                         return Mono.<Integer>just(jsonPath.read(result.getResponse().getContentAsString()));
                                     } catch (UnsupportedEncodingException e) {
                                         return Mono.error(e);
                                     }
                                 }).retry())
                                 // record each currentRate observed
                                 .subscribe(currentRate -> currentRatesHistory.updateAndGet(l -> l.append(currentRate)));

        // record the max nb of requests sent in parallel, just to be sure that the test was relevant
        // (if max concurrent calls <= rate limit then the test was useless)
        AtomicReference<List<Integer>> maxConcurrentCalls = new AtomicReference<>(List.of(0));

        // try to make each download
        RuntimeException unexpectedResultEx = new RuntimeException("Unexpected result");
        Disposable hammer = Flux.range(0, nbDownloads).flatMap(ignored -> Mono.defer(() -> {
                                                                                  // increase the nb of concurrent calls
                                                                                  maxConcurrentCalls.updateAndGet(l -> l.append(l.last() + 1));

                                                                                  // download
                                                                                  RequestBuilderCustomizer requestBuilderCustomizer = customizer()
                                                                                      // let assertion pass, I want to accumulate status codes!
                                                                                      .expect(r -> assertTrue(true));
                                                                                  String urlTemplate = FileReferenceController.FILE_PATH + FileReferenceController.DOWNLOAD_PATH;
                                                                                  String authToken = manageSecurity(getDefaultTenant(),
                                                                                                                    urlTemplate,
                                                                                                                    RequestMethod.GET,
                                                                                                                    userEmail,
                                                                                                                    getDefaultRole());
                                                                                  ResultActions res = performGet(urlTemplate,
                                                                                                                 authToken,
                                                                                                                 requestBuilderCustomizer,
                                                                                                                 "File download response status should not blow up at this point",
                                                                                                                 storedFileChecksum);
                                                                                  int status = res.andReturn().getResponse().getStatus();

                                                                                  // record the download status (200, 429, 500, other ?)
                                                                                  downloadReqStatuses.updateAndGet(l -> l.append(status));

                                                                                  // call finished, decrease the nb of concurrent calls
                                                                                  maxConcurrentCalls.updateAndGet(l -> l.append(l.last() - 1));

                                                                                  // if the result is not 200 then return and error Mono in order to retry
                                                                                  if (status == HttpStatus.OK.value()) {
                                                                                      return Mono.just(status);
                                                                                  } else {
                                                                                      return Mono.error(unexpectedResultEx).delayElement(Duration.ofMillis(200L));
                                                                                  }
                                                                              })
                                                                              // retry until we finally get the result we expect (200 download successful)
                                                                              .retryWhen(Retry.indefinitely()
                                                                                              .filter(t -> t
                                                                                                           == unexpectedResultEx))
                                                                              // use the dedicated thread pool
                                                                              .subscribeOn(Schedulers.newParallel(
                                                                                  "hammer",
                                                                                  maxConcurrency)), maxConcurrency)
                                // Spring warm up time
                                .delaySubscription(Duration.ofSeconds(15))
                                // for each OK result, count down
                                .subscribe(ignored -> latch.countDown());

        // wait for the calls to end (max 120secs)
        boolean timely = latch.await(120, TimeUnit.SECONDS);
        // free resources
        hammer.dispose();
        // cancel monitor (hammer has finished) but wait a bit so we can observe the currentRate going down
        Thread.sleep(1_000);
        monitor.dispose();

        //        LOGGER.info("concurrent=" + maxConcurrentCalls.get().mkString(","));
        //        LOGGER.info("downloadReqStatuses=" + downloadReqStatuses.get().mkString(","));
        //        LOGGER.info("rates=" + currentRatesHistory.get().mkString(","));
        //        LOGGER.info("reqs count=" + downloadReqStatuses.get().size());
        //        LOGGER.info("nb notifs=" + notificationEvents.get());

        assertTrue(
            "Test should have ended in a timely manner. Check your setup, the delay is either too short or the test took longer than expected (are you on a crowded environment?).",
            timely);
        assertTrue(
            "Test should have sent more concurrent requests than rateLimit, otherwise the whole test would be rather useless. Please check your setup.",
            maxConcurrentCalls.get().reduce(Integer::max) > rateLimit);
        assertTrue(
            "Hitting the rate limiter should have caused HTTP 429 errors and retries of the failed calls until a 200 is eventually returned, hence more calls should have been made than initially requested.",
            nbDownloads < downloadReqStatuses.get().size());
        assertTrue(
            "Observed rate history should never have exceeded the rate limit (in this single node setting at least).",
            currentRatesHistory.get().reduce(Integer::max) <= rateLimit);
        assertFalse("Each time the rate went up above 0 it should eventually have gone back to zero.",
                    currentRatesHistory.get().foldLeft(false, (aboveZero, next) -> next > 0));
        try {
            Awaitility.await().atMost(15, TimeUnit.SECONDS).until(() -> {
                // there's been many notifications send for that
                return notificationEvents.get() > 1;
            });
        } catch (ConditionTimeoutException e) {
            fail("Several notification should have been sent on quota exceeded");
        }

    }

}
