package fr.cnes.regards.modules.feature.service.session;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.feature.service.task.FeatureTaskScheduler;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FeatureDeleteJob extends AbstractJob<Void> {

    private static final String SOURCE_NAME_PARAM = "sourceName";

    private static final String SESSION_NAME_PARAM = "sessionName";

    private String sourceName;

    private String sessionName;

    @Autowired
    private FeatureDeleteService featureDeleteService;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    public static Set<JobParameter> getParameters(String source, Optional<String> sessionOptional) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(SOURCE_NAME_PARAM, source));
        sessionOptional.ifPresent(session -> parameters.add(new JobParameter(SESSION_NAME_PARAM, session)));
        return parameters;
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        sourceName = getValue(parameters, SOURCE_NAME_PARAM);
        sessionName = (String) getOptionalValue(parameters, SESSION_NAME_PARAM).orElse(null);
    }

    @Override
    public void run() {
        logger.trace("[{}] FeatureDeleteJob starts for source {}", jobInfoId, sourceName);
        try {
            lockingTaskExecutors.executeWithLock(deleteTask,
                                                 new LockConfiguration(Instant.now(),
                                                                       FeatureTaskScheduler.DELETE_REQUEST_LOCK,
                                                                       Duration.ofSeconds(300),
                                                                       Duration.ZERO));
        } catch (Throwable throwable) {
            logger.error("Error during deletion job process", throwable);
        }
    }

    private final LockingTaskExecutor.Task deleteTask = () -> {
        lockingTaskExecutors.assertLocked();
        featureDeleteService.scheduleDeleteRequests(sourceName, sessionName);
    };

}
