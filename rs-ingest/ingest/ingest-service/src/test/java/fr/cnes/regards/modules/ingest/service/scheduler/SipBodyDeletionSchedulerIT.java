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
package fr.cnes.regards.modules.ingest.service.scheduler;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dao.SipDeletionSchedulerRepository;
import fr.cnes.regards.modules.ingest.domain.scheduler.SipDeletionSchedulerEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.ingest.service.job.SIPBodyDeletionJob;
import fr.cnes.regards.modules.ingest.service.settings.IngestSettingsService;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import fr.cnes.regards.modules.ingest.service.sip.scheduler.SIPBodyDeletionRequestScheduler;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

/**
 * @author Thomas GUILLOU
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest_test_sip_body_deletion" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "noscheduler", "nojobs" })
public class SipBodyDeletionSchedulerIT extends IngestMultitenantServiceIT {

    protected static final String SESSION_OWNER = "SESSION_OWNER";

    protected static final String SESSION = "session";

    private static final Set<String> CATEGORIES = Sets.newHashSet("CATEGORY");

    private static final int NUMBER_OF_SIP = 4;

    /**
     * Use a reference date instead of now(), to test exactly the same input each time
     * reference date is : 01/01/2022 at 00:00:00.0000 UTC + 0
     */
    private static final OffsetDateTime REFERENCE_DATE = OffsetDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private SipDeletionSchedulerRepository sipDeletionSchedulerRepository;

    @Autowired
    private SIPBodyDeletionRequestScheduler sipBodyDeletionRequestScheduler;

    @Autowired
    protected IJobService jobService;

    @Autowired
    private IngestSettingsService ingestSettingsService;

    private SIPEntity createSIP(int number) {
        var sip = new SIPEntity();
        sip.setProviderId("SIP_" + number);
        sip.setCreationDate(REFERENCE_DATE);
        sip.setLastUpdate(REFERENCE_DATE.minusDays(number));
        sip.setSessionOwner(SESSION_OWNER);
        sip.setSession(SESSION);
        sip.setCategories(CATEGORIES);
        sip.setState(SIPState.STORED);
        sip.setVersion(1);
        sip.setChecksum("1234567890" + number);
        sip.setSipId("test_" + number);
        SIPDto rawsip = SIPDto.build(EntityType.DATA, "SIP_" + number);
        sip.setSip(rawsip);
        return sip;
    }

    @Override
    public void doInit() throws EntityException {
        sipRepository.deleteAll();
        // The repository is cleaned and refilled at each test running
        List<SIPEntity> sips = IntStream.range(0, NUMBER_OF_SIP).mapToObj(this::createSIP).toList();
        sipRepository.saveAll(sips);
        ingestSettingsService.setSipBodyTimeToLive(7);
    }

    /**
     * Check if data input are correctly added in bd
     */
    @Test
    public void doInitTest() {
        Assert.assertEquals(NUMBER_OF_SIP, sipRepository.count());
        Optional<SIPEntity> sipEntity = sipRepository.findOneBySipId("test_" + (NUMBER_OF_SIP - 1));
        Assert.assertTrue(sipEntity.isPresent());
        // lastUpdate is stored with different zoneOffset (utc + 1 or UTC +2) so force it to UTC = 0;
        Assert.assertEquals(REFERENCE_DATE.minusDays(NUMBER_OF_SIP - 1),
                            sipEntity.get().getLastUpdate().withOffsetSameInstant(ZoneOffset.UTC));

    }

    /**
     * Test if service correctly delete SIP
     */
    @Test
    public void deletionTest() {
        // Given
        OffsetDateTime lowerDate = REFERENCE_DATE.minusDays(3);
        OffsetDateTime upperDate = REFERENCE_DATE.minusDays(1);
        // When
        // lowerDate is excluded, and upperDate included
        int nbrSipUpdated = sipService.cleanOldRawSip(lowerDate, upperDate);
        // Then
        Assert.assertEquals(nbrSipUpdated, 2);
        List<SIPEntity> all = sipRepository.findAllByOrderByLastUpdateAsc();
        // First element is the older (lastUpdate = REFERENCE_DATE -3),
        // his lastUpdate is equals to lower bound date, which is EXCLUDED of the query
        Assert.assertNotNull(all.get(0).getSip());
        // Second element (lastUpdate = REFERENCE_DATE -2
        // is included in the query because is between date bounds
        Assert.assertNull(all.get(1).getSip());
        // Third element (lastUpdate = REFERENCE_DATE -1)
        // his lastUpdate is equals to upper bound date, which is INCLUDED in the query
        Assert.assertNull(all.get(2).getSip());
        // Last element (lastUpdate = REFERENCE_DATE)
        // is excluded of the query because is outside of date bounds
        Assert.assertNotNull(all.get(3).getSip());
    }

    /**
     * Check if service don't delete SIP if state is INGESTED
     */
    @Test
    public void wrongStateDeletionTest() {
        // Given
        OffsetDateTime lowerDate = REFERENCE_DATE.minusDays(3);
        OffsetDateTime upperDate = REFERENCE_DATE.minusDays(1);
        List<SIPEntity> allBeforeTest = sipRepository.findAllByOrderByLastUpdateAsc();
        allBeforeTest.get(1).setState(SIPState.INGESTED);
        allBeforeTest.get(2).setState(SIPState.INGESTED);
        sipRepository.saveAll(allBeforeTest);
        // When
        int nbrSipUpdated = sipService.cleanOldRawSip(lowerDate, upperDate);
        // Then
        // none have been updated because
        // element 1 and 2 : State is not STORED or DELETED.
        // element 0 and 3 : outside of bounds
        Assert.assertEquals(0, nbrSipUpdated);
        List<SIPEntity> all = sipRepository.findAll();
        Assert.assertTrue(all.stream().allMatch(sip -> sip.getSip() != null));
    }

    @Test
    public void testRepository() {
        sipDeletionSchedulerRepository.deleteAll();
        Optional<SipDeletionSchedulerEntity> sipSchedulerEntityOpt = sipDeletionSchedulerRepository.findFirst();
        Assert.assertFalse(sipSchedulerEntityOpt.isPresent());
        SipDeletionSchedulerEntity sipSchedulerEntity = new SipDeletionSchedulerEntity();
        sipSchedulerEntity.setLastScheduledDate(OffsetDateTime.now());
        sipDeletionSchedulerRepository.save(sipSchedulerEntity);
        Assert.assertTrue(sipDeletionSchedulerRepository.findFirst().isPresent());
    }

    /**
     * Check if the date stored by the scheduler table is correct
     */
    @Test
    public void testLastScheduledDateWellUpdated() throws ExecutionException, InterruptedException {
        // Given
        OffsetDateTime lowerDate = REFERENCE_DATE.minusDays(3);
        OffsetDateTime upperDate = REFERENCE_DATE.minusDays(1);
        JobInfo jobInfo = sipBodyDeletionRequestScheduler.scheduleJob();
        jobInfo.getParameters().clear();
        jobInfo.setParameters(Sets.newHashSet(new JobParameter(SIPBodyDeletionJob.LAST_SCHEDULED_DATE_PARAMETER,
                                                               lowerDate),
                                              new JobParameter(SIPBodyDeletionJob.CLOSEST_DATE_TO_DELETE_PARAMETER,
                                                               upperDate)));
        Assert.assertNotNull("A job should be created", jobInfo);
        // When
        jobService.runJob(jobInfo, getDefaultTenant()).get();
        // Then
        Optional<SipDeletionSchedulerEntity> schedulerEntity = sipDeletionSchedulerRepository.findFirst();
        Assert.assertTrue(schedulerEntity.isPresent());
        // lastUpdate is stored with different zoneOffset (utc + 1 or UTC +2) so force it to UTC = 0;
        Assert.assertEquals(upperDate,
                            schedulerEntity.get().getLastScheduledDate().withOffsetSameInstant(ZoneOffset.UTC));
    }

    /**
     * Check if only one job can be running at the same time
     */
    @Test
    public void testJobConcurrence() {
        JobInfo jobInfo = sipBodyDeletionRequestScheduler.scheduleJob();
        Assert.assertNotNull("A job should be created", jobInfo);
        jobService.runJob(jobInfo, getDefaultTenant());
        JobInfo anotherJobInfo = sipBodyDeletionRequestScheduler.scheduleJob();
        Assert.assertNull("A job mustn't be created because another one is running", anotherJobInfo);
    }

    /**
     * Check if no Sip are deleted if SipBodyTimeToLive is -1
     */
    @Test
    public void testDisableSipDeletion() throws EntityException {
        // Given
        ingestSettingsService.setSipBodyTimeToLive(-1);
        // When
        JobInfo jobInfo = sipBodyDeletionRequestScheduler.scheduleJob();
        // Then
        Assert.assertNull("A job should be created", jobInfo);
        List<SIPEntity> all = sipRepository.findAllByOrderByLastUpdateAsc();
        Assert.assertEquals(NUMBER_OF_SIP, all.stream().count());
    }
}
