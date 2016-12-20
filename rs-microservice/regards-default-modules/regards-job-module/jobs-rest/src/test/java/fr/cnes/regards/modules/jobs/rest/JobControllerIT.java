/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
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

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.service.service.IJobInfoService;

/**
 * 
 * @author Christophe Mertz
 *
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@ContextConfiguration(classes = { JobControllerTestConfiguration.class })
public class JobControllerIT extends AbstractRegardsIT {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JobControllerIT.class);

    /**
     * 
     */
    @Autowired
    private IJobInfoService jobInfoService;

    private final String apiJobs = "/jobs";

    private final String apiAJob = apiJobs + "/{jobId}";

    private final String apiAJobResults = apiAJob + "/results";

    private final String apiJobsState = apiJobs + "/state/{state}";

    @Test
    public void getAllJobs() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_STAR,
                                                        Matchers.hasSize(jobInfoService.retrieveJobInfoList().size())));
        expectations.add(MockMvcResultMatchers.jsonPath("$.*.links", Matchers.notNullValue()));
        performDefaultGet(apiJobs, expectations, "unable to load all jobs");
    }

    @Test
    public void getOneJob() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        final JobInfo aJob = jobInfoService.retrieveJobInfoList().get(0);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.id", Matchers.hasToString(aJob.getId().toString())));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.owner", Matchers.hasToString(aJob.getOwner())));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.priority",
                                                        Matchers.hasToString(String.format("%s", aJob.getPriority()))));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.content.className", Matchers.hasToString(aJob.getClassName())));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.archived",
                                                        Matchers.hasToString(aJob.isArchived().toString())));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.status.status",
                                                        Matchers.hasToString(aJob.getStatus().getJobStatus().name())));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.status.description",
                                                        Matchers.hasToString(aJob.getStatus().getDescription())));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_LINKS, Matchers.notNullValue()));

        performDefaultGet(apiAJob, expectations, String.format("unable to load the job <%s>", aJob.getId()),
                          aJob.getId());
    }

    @Test
    public void getJobResults() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        final JobInfo aJob = jobInfoService.retrieveJobInfoList().get(0);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT, hasSize(aJob.getResult().size())));
        performDefaultGet(apiAJobResults, expectations, String.format("unable to get job's result <%s>", aJob.getId()),
                          aJob.getId());
    }

    @Test
    public void getJobsState() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());

        performDefaultGet(apiJobsState, expectations,
                          String.format("unable to get jobs with status  <%s>", JobStatus.RUNNING), JobStatus.RUNNING);
    }

    @Test
    public void deleteAJob() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        final Long jobId = jobInfoService.retrieveJobInfoList().get(0).getId();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.links", Matchers.notNullValue()));
        
        performDefaultDelete(apiAJob, expectations, String.format("unable to stop the job <%s>", jobId), jobId);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
