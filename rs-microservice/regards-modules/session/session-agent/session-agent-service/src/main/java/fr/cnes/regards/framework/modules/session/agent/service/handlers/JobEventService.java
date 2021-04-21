package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatusInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.session.agent.service.update.AgentSnapshotJob;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for {@link JobEventHandler}. Update {@link SnapshotProcess} according to the {@link AgentSnapshotJob}
 * status
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class JobEventService {

    @Autowired
    private ISnapshotProcessRepository snapshotRepo;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    /**
     * Handle event by calling the listener method associated to the event type.
     *
     * @param events {@link JobEvent}s
     */
    public void updateSnapshotProcess(List<JobEvent> events) {
        // Filter out all jobs not related to AgentSnapshotJob ids saved in snapshot processes repo
        List<UUID> jobIds = events.stream().map(JobEvent::getJobId).collect(Collectors.toList());
        Set<SnapshotProcess> snapshotProcesses = this.snapshotRepo.findByJobIdIn(jobIds);

        // Update snapshot processes
        for (SnapshotProcess snapshotProcess : snapshotProcesses) {
            Optional<JobInfo> jobInfoOpt = this.jobInfoRepo.findById(snapshotProcess.getJobId());

            if (jobInfoOpt.isPresent()) {
                JobInfo jobInfo = jobInfoOpt.get();
                JobStatusInfo jobInfoStatus = jobInfo.getStatus();

                switch (jobInfoStatus.getStatus()) {
                    case SUCCEEDED:
                        snapshotProcess.setJobId(null);
                        snapshotProcess.setLastUpdate(jobInfoStatus.getStopDate());
                        break;
                    case FAILED:
                    case ABORTED:
                        snapshotProcess.setJobId(null);
                        break;
                    default:
                        break;
                }
            }
        }
        this.snapshotRepo.saveAll(snapshotProcesses);
    }
}
