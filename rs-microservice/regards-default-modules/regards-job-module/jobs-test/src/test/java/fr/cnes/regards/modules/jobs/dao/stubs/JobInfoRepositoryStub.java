/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.jobs.dao.stubs;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.test.repository.RepositoryStub;
import fr.cnes.regards.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.modules.jobs.domain.JobConfiguration;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobParameters;
import fr.cnes.regards.modules.jobs.domain.JobParametersFactory;
import fr.cnes.regards.modules.jobs.domain.JobStatus;

/***
 * {@link PluginConfiguration} Repository stub.
 *
 * @author Christophe Mertz
 *
 */
@Repository
@Primary
@Profile("test")
public class JobInfoRepositoryStub extends RepositoryStub<JobInfo> implements IJobInfoRepository {

    public JobInfoRepositoryStub() {
        jobInfo1.getStatus().setJobStatus(JobStatus.RUNNING);
        getEntities().add(jobInfo1);
        jobInfo1.getStatus().setJobStatus(JobStatus.QUEUED);
        getEntities().add(jobInfo2);
    }

    @Override
    public List<JobInfo> findAllByStatusStatus(JobStatus pStatus) {
        try (Stream<JobInfo> stream = getEntities().stream()) {
            final List<JobInfo> jobs = new ArrayList<>();
            stream.filter(j -> j.getStatus().getJobStatus().equals(pStatus)).forEach(j -> jobs.add(j));
            return jobs;
        }
    }

    private static final JobParameters jobParameters1 = JobParametersFactory.build().addParameter("param11", "value11")
            .addParameter("param12", "value12").addParameter("param13", "value13").getParameters();

    private final static Path workspace = FileSystems.getDefault().getPath("/home", "cmertz", "git-regards", "rs-microservice");

    private static JobConfiguration jobConfiguration1 = new JobConfiguration("", jobParameters1,
            "fr.cnes.regards.modules.MyCustomJob", LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(15),
            33, workspace, "owner@unix.org");

    private static final JobInfo jobInfo1 = new JobInfo(jobConfiguration1);

    private static final JobParameters jobParameters2 = JobParametersFactory.build()
            .addParameter("parameter22", "parameter value22").addParameter("parameter22", "parameter value22")
            .addParameter("parameter32", "parameter value32").addParameter("parameter42", "parameter value42")
            .getParameters();

    private static JobConfiguration jobConfiguration2 = new JobConfiguration("", jobParameters2,
            "fr.cnes.regards.modules.MyOtherCustomJob", LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(15),
            10, workspace, "master@ubuntu.org");
    
    private static final JobInfo jobInfo2 = new JobInfo(jobConfiguration2);

}
