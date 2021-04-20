package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for {@link JobEventHandler}
 *
 * @author Iliana Ghazali
 **/
@Service
@RegardsTransactional
public class JobEventService {

    @Autowired
    private ISnapshotProcessRepository snapshotRepo;

    @Autowired
    private IJobInfoRepository jobInfo;

    /**
     * Handle event by calling the listener method associated to the event type.
     *
     * @param events {@link JobEvent}s
     */
    public void handle(List<JobEvent> events) {
        // Update SnapshotProcesses only if linked job is ending in success state
        for(JobEvent jobEvent : events) {
            if(jobEvent.getJobEventType().equals(JobEventType.SUCCEEDED)) {
                UUID jobId = jobEvent.getJobId();
                Optional<SnapshotProcess> snapshotOpt = this.snapshotRepo.findByJobId(jobId);
                Optional<JobInfo> jobInfo = this.jobInfo.findById(jobId);
                // If job is linked to SnapshotProcess, update SnapshotProcess
                if(snapshotOpt.isPresent() && jobInfo.isPresent()) {
                    SnapshotProcess snapshot = snapshotOpt.get();
                    snapshot.setJobId(null);
                    snapshot.setLastUpdate(jobInfo.get().getStatus().getStopDate());
                }
            }
        }
    }

}
