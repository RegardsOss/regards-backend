/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultMatcher;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
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

    private final String apiAJob = apiJobs + "{jobId}";

    private final String apiAJobResults = apiAJob+"/results";

    private final String apiJobsState = apiJobs + "/state/{state}";
    
    @Test
    @Ignore
    public void getAllJobs() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        LOGGER.debug("job count : " + jobInfoService.retrieveJobInfoList().size());
        expectations.add(status().isOk());
        performDefaultGet(apiJobs, expectations, "unable to load all jobs");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
