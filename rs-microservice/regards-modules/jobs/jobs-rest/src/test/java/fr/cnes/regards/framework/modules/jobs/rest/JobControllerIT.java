/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jobs.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * @author Christophe Mertz
 *
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@ContextConfiguration(classes = { JobControllerTestConfiguration.class })
@Ignore
public class JobControllerIT extends AbstractRegardsIT {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JobControllerIT.class);

    /**
     * Generated token for tests
     */
    private static String token = "";

    @Autowired
    private IJobInfoService jobInfoService;

    @Before
    public void init() {

        manageDefaultSecurity(JobController.JOBS, RequestMethod.GET);
        manageDefaultSecurity(JobController.JOBS + "/{jobId}", RequestMethod.GET);
        manageDefaultSecurity(JobController.JOBS + "/{jobId}/results", RequestMethod.GET);

        token = generateToken(DEFAULT_USER_EMAIL, DEFAULT_ROLE);
    }

    @Test
    public void getAllJobs() {
        RequestBuilderCustomizer requestBuilderCustomizer = getRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_STAR,
                                                        Matchers.hasSize(jobInfoService.retrieveJobs().size())));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$.*.links", Matchers.notNullValue()));

        performGet(JobController.JOBS, token, requestBuilderCustomizer, "unable to load all jobs");
    }

    @Test
    public void getOneJob() {
        final JobInfo aJob = jobInfoService.retrieveJobs().get(0);
        RequestBuilderCustomizer requestBuilderCustomizer = getRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".id",
                                                        Matchers.hasToString(aJob.getId().toString())));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".owner",
                                                        Matchers.hasToString(aJob.getOwner())));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".priority",
                                                        Matchers.hasToString(String.format("%s", aJob.getPriority()))));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".className",
                                                        Matchers.hasToString(aJob.getClassName())));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".status.status",
                                                        Matchers.hasToString(aJob.getStatus().getStatus().name())));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + ".status.description",
                                                        Matchers.hasToString(aJob.getDescription())));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS, Matchers.notNullValue()));

        performGet(JobController.JOBS + "/{jobId}", token, requestBuilderCustomizer,
                   String.format("unable to load the job <%s>", aJob.getId()), aJob.getId());
    }

//    @Test
//    public void getJobResults() {
//        final List<ResultMatcher> expectations = new ArrayList<>();
//        final JobInfo aJob = jobInfoService.retrieveJobs().get(0);
//        RequestBuilderCustomizer requestBuilderCustomizer = getRequestBuilderCustomizer();
//        requestBuilderCustomizer.addExpectation(status().isOk());
//        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT, hasSize(aJob.getResults().size())));
//        performGet(JobController.JOBS + "/{jobId}/results", token, requestBuilderCustomizer,
//                   String.format("unable to get job's result <%s>", aJob.getId()), aJob.getId());
//    }

    @Test
    public void getJobsState() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());

        performDefaultGet(JobController.JOBS + "/state/{state}", expectations,
                          String.format("unable to get jobs with status  <%s>", JobStatus.RUNNING), JobStatus.RUNNING);
    }

    @Test
    public void deleteAJob() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        final UUID jobId = jobInfoService.retrieveJobs().get(0).getId();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.links", Matchers.notNullValue()));

        performDefaultDelete(JobController.JOBS + "/{jobId}", expectations,
                             String.format("unable to stop the job <%s>", jobId), jobId);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
