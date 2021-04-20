package fr.cnes.regards.framework.modules.session.agent.service.update;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import java.time.OffsetDateTime;
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

    public static final String FREEZE_DATE = "FREEZE_DATE";

    public static final String SNAPSHOT_PROCESS = "SNAPSHOT_PROCESS";

    private SnapshotProcess snapshotProcess;

    private OffsetDateTime freezeDate;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        this.snapshotProcess = getValue(parameters, SNAPSHOT_PROCESS, new TypeToken<SnapshotProcess>() {}.getType());

        this.freezeDate = getValue(parameters, FREEZE_DATE, new TypeToken<OffsetDateTime>() {}.getType());
    }

    @Override
    public void run() {
        String source = snapshotProcess.getSource();
        logger.debug("[{}] AgentSnapshot job starts for source {}", jobInfoId, source);
        long start = System.currentTimeMillis();
        int nbSessionStep = agentSnapshotService.generateSessionStep(snapshotProcess, freezeDate);
        logger.debug("[{}] AgentSnapshot job ends in {} ms. {} session step created for source {}", jobInfoId,
                     System.currentTimeMillis() - start, nbSessionStep, source);
    }
}
