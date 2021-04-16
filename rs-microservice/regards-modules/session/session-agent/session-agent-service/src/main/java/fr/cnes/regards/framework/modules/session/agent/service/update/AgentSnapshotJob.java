package fr.cnes.regards.framework.modules.session.agent.service.update;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Iliana Ghazali
 */
public class AgentSnapshotJob extends AbstractJob<Void> {

    @Autowired
    protected IJobInfoService jobInfoService;

    @Autowired
    private AgentSnapshotService agentSnapshotService;

    public static final String STEP_EVENTS = "STEP_EVENTS";

    public static final String SOURCE = "SOURCE";

    private List<StepPropertyUpdateRequest> stepPropertyEvent;

    private String source;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        this.stepPropertyEvent = getValue(parameters, STEP_EVENTS, new TypeToken<List<StepPropertyUpdateRequest>>() {

        }.getType());

        this.source = getValue(parameters, STEP_EVENTS, new TypeToken<String>() {

        }.getType());
    }

    @Override
    public void run() {
        logger.debug("[{}] AgentSnapshot job starts for source {}", jobInfoId, source);
        long start = System.currentTimeMillis();
        int nbSessionStep = agentSnapshotService.generateSessionStep(source, stepPropertyEvent);
        logger.debug("[{}] AgentSnapshot job ends in {} ms. {} session step created for source {}", jobInfoId,
                     System.currentTimeMillis() - start, nbSessionStep, source);
    }
}
